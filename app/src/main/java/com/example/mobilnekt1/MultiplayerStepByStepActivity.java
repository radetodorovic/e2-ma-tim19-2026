package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.games.stepbystep.StepByStepEngine;
import com.example.mobilnekt1.games.stepbystep.StepPuzzle;
import com.example.mobilnekt1.games.stepbystep.StepPuzzleRepository;
import com.example.mobilnekt1.games.stepbystep.multiplayer.StepGameListener;
import com.example.mobilnekt1.games.stepbystep.multiplayer.StepGameRepository;
import com.example.mobilnekt1.games.stepbystep.multiplayer.StepGameState;

import java.util.Locale;

public final class MultiplayerStepByStepActivity extends BaseKt1Activity {
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private StepGameRepository repository;
    private String matchId;
    private StepGameState state;
    private LinearLayout hintsContainer;
    private TextView roundView;
    private TextView turnView;
    private TextView timerView;
    private TextView pointsView;
    private TextView scoreView;
    private EditText answerInput;
    private Button confirmButton;
    private boolean resultShown;
    private boolean advancing;
    private boolean submitting;
    private long handledEventVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_by_step);
        matchId = getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID);
        if (matchId == null) {
            finish();
            return;
        }
        bindViews();
        repository = new StepGameRepository(this);
        confirmButton.setOnClickListener(v -> submitAnswer());
        repository.listen(matchId, new StepGameListener() {
            @Override
            public void onChanged(StepGameState newState) {
                state = newState;
                advancing = false;
                submitting = false;
                render();
                showRemoteEvent(newState);
            }

            @Override
            public void onError(String message) {
                showInfoDialog(getString(R.string.match_error_title), message);
            }
        });
        repository.initialize(matchId, silentCallback());
        ticker.post(tick);
    }

    private void showRemoteEvent(StepGameState newState) {
        if (newState.eventVersion <= handledEventVersion) {
            return;
        }
        handledEventVersion = newState.eventVersion;
        String currentUserId = repository.currentUserId();
        if (!"correctAnswer".equals(newState.eventType)
                || currentUserId == null
                || currentUserId.equals(newState.eventPlayerId)) {
            return;
        }
        showInfoDialog(getString(R.string.opponent_correct_title),
                getString(R.string.opponent_correct_message, (int) newState.eventPoints,
                        newState.isFinished()
                                ? getString(R.string.game_finished)
                                : getString(R.string.next_round_started)));
    }

    private void bindViews() {
        hintsContainer = findViewById(R.id.container_hints);
        roundView = findViewById(R.id.text_step_round);
        turnView = findViewById(R.id.text_step_turn);
        timerView = findViewById(R.id.text_step_timer);
        pointsView = findViewById(R.id.text_step_points);
        scoreView = findViewById(R.id.text_step_score);
        answerInput = findViewById(R.id.input_step_answer);
        confirmButton = findViewById(R.id.button_confirm_step_answer);
    }

    private void submitAnswer() {
        String answer = answerInput.getText().toString().trim();
        if (answer.isEmpty()) {
            answerInput.setError(getString(R.string.required_field));
            return;
        }
        confirmButton.setEnabled(false);
        submitting = true;
        confirmButton.setText(R.string.checking_answer);
        ticker.postDelayed(() -> {
            if (submitting) {
                submitting = false;
                render();
                showInfoDialog(getString(R.string.match_error_title),
                        getString(R.string.request_timeout));
            }
        }, 8_000);
        repository.submitAnswer(matchId, answer, new GameActionCallback() {
            @Override
            public void onSuccess() {
                submitting = false;
                answerInput.setText("");
            }

            @Override
            public void onError(String message) {
                submitting = false;
                confirmButton.setEnabled(true);
                confirmButton.setText(R.string.confirm_answer);
                showInfoDialog(getString(R.string.step_answer_rejected), message);
            }
        });
    }

    private void render() {
        if (state == null) {
            return;
        }
        int openedHints = openedHints();
        StepPuzzle puzzle = StepPuzzleRepository.forRound(state.puzzleIndex);
        roundView.setText(getString(R.string.round_value, state.round + 1, 2));
        boolean myTurn = repository.currentUserId() != null
                && repository.currentUserId().equals(state.activePlayerId);
        if (state.isFinished()) {
            turnView.setText(R.string.game_finished);
        } else if ("steal".equals(state.phase)) {
            turnView.setText(myTurn ? R.string.your_steal_turn : R.string.opponent_steal_turn);
        } else {
            turnView.setText(myTurn ? R.string.your_turn : R.string.opponent_turn);
        }
        pointsView.setText(getString(R.string.possible_points,
                "steal".equals(state.phase) ? 5 : StepByStepEngine.pointsForHint(openedHints)));
        scoreView.setText(getString(R.string.two_player_score,
                (int) state.player1Score, (int) state.player2Score));
        answerInput.setEnabled(myTurn && !state.isFinished());
        confirmButton.setEnabled(myTurn && !state.isFinished() && !submitting);
        confirmButton.setText(submitting ? R.string.checking_answer : R.string.confirm_answer);
        renderHints(puzzle, openedHints);
        if (state.isFinished() && !resultShown) {
            resultShown = true;
            showFinishDialog(getString(R.string.game_result), getString(R.string.two_player_score,
                    (int) state.player1Score, (int) state.player2Score));
        }
    }

    private int openedHints() {
        if (state == null || "steal".equals(state.phase) || state.isFinished()) {
            return StepByStepEngine.HINT_COUNT;
        }
        long remaining = Math.max(0, state.deadlineMillis - System.currentTimeMillis());
        int elapsed = StepByStepEngine.ROUND_SECONDS - (int) Math.ceil(remaining / 1000.0);
        return Math.min(7, 1 + Math.max(0, elapsed) / 10);
    }

    private void renderHints(StepPuzzle puzzle, int openedHints) {
        hintsContainer.removeAllViews();
        for (int i = 0; i < StepByStepEngine.HINT_COUNT; i++) {
            TextView view = new TextView(this);
            view.setText(i < openedHints
                    ? getString(R.string.numbered_hint, i + 1, puzzle.hints[i])
                    : getString(R.string.hidden_hint, i + 1));
            view.setTextColor(getResources().getColor(R.color.text_primary));
            view.setTextSize(16);
            view.setPadding(16, 12, 16, 12);
            view.setBackgroundResource(i < openedHints
                    ? R.drawable.card_background : R.drawable.input_background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 10);
            hintsContainer.addView(view, params);
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (state != null && !state.isFinished()) {
                long remaining = Math.max(0, state.deadlineMillis - System.currentTimeMillis());
                long seconds = (long) Math.ceil(remaining / 1000.0);
                timerView.setText(String.format(Locale.getDefault(), "%02d:%02d",
                        seconds / 60, seconds % 60));
                renderHints(StepPuzzleRepository.forRound(state.puzzleIndex), openedHints());
                if (remaining == 0 && !advancing) {
                    advancing = true;
                    repository.advanceExpired(matchId, silentCallback());
                }
            }
            ticker.postDelayed(this, 250);
        }
    };

    private GameActionCallback silentCallback() {
        return new GameActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                advancing = false;
                showInfoDialog(getString(R.string.match_error_title), message);
            }
        };
    }

    @Override
    protected void onDestroy() {
        ticker.removeCallbacksAndMessages(null);
        repository.stopListening();
        super.onDestroy();
    }
}
