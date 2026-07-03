package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mobilnekt1.games.stepbystep.StepByStepEngine;
import com.example.mobilnekt1.games.stepbystep.StepPuzzle;
import com.example.mobilnekt1.games.content.GameContentRepository;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import java.util.List;

import java.util.Locale;

public class StepByStepActivity extends BaseKt1Activity {
    private LinearLayout hintsContainer;
    private TextView roundView;
    private TextView turnView;
    private TextView timerView;
    private TextView pointsView;
    private TextView scoreView;
    private EditText answerInput;
    private Button confirmButton;

    private CountDownTimer timer;
    private StepPuzzle puzzle;
    private int roundIndex;
    private int openedHints = 1;
    private int playerOneScore;
    private int playerTwoScore;
    private boolean stealPhase;
    private long remainingMillis;
    private boolean challengeMode;
    private List<StepPuzzle> puzzles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        challengeMode = getIntent().getBooleanExtra(EXTRA_CHALLENGE_GAME, false);
        setContentView(R.layout.activity_step_by_step);

        hintsContainer = findViewById(R.id.container_hints);
        roundView = findViewById(R.id.text_step_round);
        turnView = findViewById(R.id.text_step_turn);
        timerView = findViewById(R.id.text_step_timer);
        pointsView = findViewById(R.id.text_step_points);
        scoreView = findViewById(R.id.text_step_score);
        answerInput = findViewById(R.id.input_step_answer);
        confirmButton = findViewById(R.id.button_confirm_step_answer);
        confirmButton.setOnClickListener(v -> checkAnswer());

        new GameContentRepository(FirebaseProvider.getInstance(this).getFirestore())
                .loadStepPuzzles(new GameContentRepository.Callback<List<StepPuzzle>>() {
                    @Override public void onSuccess(List<StepPuzzle> value) {
                        puzzles = value;
                        if (savedInstanceState != null) {
                            roundIndex = savedInstanceState.getInt("roundIndex");
                            openedHints = savedInstanceState.getInt("openedHints", 1);
                            playerOneScore = savedInstanceState.getInt("playerOneScore");
                            playerTwoScore = savedInstanceState.getInt("playerTwoScore");
                            stealPhase = savedInstanceState.getBoolean("stealPhase");
                            remainingMillis = savedInstanceState.getLong("remainingMillis");
                            puzzle = puzzles.get(roundIndex % puzzles.size()); render();
                            startTimer(Math.max(1000, remainingMillis));
                        } else startRound();
                    }
                    @Override public void onError(String message) {
                        showFinishDialog(getString(R.string.step_by_step_title), message);
                    }
                });
    }

    private void startRound() {
        cancelTimer();
        puzzle = puzzles.get(roundIndex % puzzles.size());
        openedHints = 1;
        stealPhase = false;
        remainingMillis = StepByStepEngine.ROUND_SECONDS * 1000L;
        answerInput.setText("");
        answerInput.setEnabled(true);
        confirmButton.setEnabled(true);
        render();
        startTimer(remainingMillis);
    }

    private void startTimer(long durationMillis) {
        cancelTimer();
        timer = new CountDownTimer(durationMillis, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                if (!stealPhase) {
                    int elapsedSeconds = StepByStepEngine.ROUND_SECONDS
                            - (int) Math.ceil(millisUntilFinished / 1000.0);
                    int expectedHints = Math.min(StepByStepEngine.HINT_COUNT, 1 + elapsedSeconds / 10);
                    if (expectedHints != openedHints) {
                        openedHints = expectedHints;
                        renderHints();
                        renderPoints();
                    }
                }
                renderTimer();
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                if (stealPhase) {
                    finishRound(false);
                } else if (challengeMode) {
                    finishRound(false);
                } else {
                    beginStealPhase();
                }
            }
        }.start();
    }

    private void checkAnswer() {
        String answer = answerInput.getText().toString().trim();
        if (answer.isEmpty()) {
            answerInput.setError(getString(R.string.required_field));
            return;
        }
        if (StepByStepEngine.matches(answer, puzzle.solution)) {
            int scorer = challengeMode ? 1 : (stealPhase ? otherPlayer() : activePlayer());
            int points = stealPhase ? 5 : StepByStepEngine.pointsForHint(openedHints);
            addScore(scorer, points);
            showInfoDialog(getString(R.string.correct_answer_title), challengeMode
                    ? "Osvojili ste " + points + " bodova."
                    : getString(R.string.step_points_awarded, scorer, points));
            finishRound(true);
        } else {
            answerInput.setText("");
            showToast(R.string.incorrect_answer);
        }
    }

    private void beginStealPhase() {
        stealPhase = true;
        openedHints = StepByStepEngine.HINT_COUNT;
        remainingMillis = StepByStepEngine.STEAL_SECONDS * 1000L;
        answerInput.setText("");
        render();
        showToast(R.string.step_steal_started);
        startTimer(remainingMillis);
    }

    private void finishRound(boolean solved) {
        cancelTimer();
        answerInput.setEnabled(false);
        confirmButton.setEnabled(false);
        if (!solved) {
            showInfoDialog(getString(R.string.round_result),
                    getString(R.string.step_round_unsolved, puzzle.solution));
        }
        if (roundIndex == 0) {
            roundIndex = 1;
            hintsContainer.postDelayed(this::startRound, solved ? 900 : 1600);
        } else {
            hintsContainer.postDelayed(this::showGameResult, solved ? 900 : 1600);
        }
    }

    private void showGameResult() {
        if (challengeMode) {
            showGameFinishDialog(getString(R.string.game_result),
                    "Korak po korak je zavrsen.\nVasi bodovi: " + playerOneScore, playerOneScore);
            return;
        }
        String winner;
        if (playerOneScore == playerTwoScore) {
            winner = getString(R.string.draw_result);
        } else {
            winner = getString(R.string.player_wins,
                    playerOneScore > playerTwoScore ? 1 : 2);
        }
        showGameFinishDialog(getString(R.string.game_result),
                getString(R.string.two_player_score, playerOneScore, playerTwoScore) + "\n" + winner,
                playerOneScore);
    }

    private int activePlayer() {
        return roundIndex + 1;
    }

    private int otherPlayer() {
        return activePlayer() == 1 ? 2 : 1;
    }

    private void addScore(int player, int points) {
        if (player == 1) {
            playerOneScore += points;
        } else {
            playerTwoScore += points;
        }
        renderScore();
    }

    private void render() {
        roundView.setText(getString(R.string.round_value, roundIndex + 1, 2));
        turnView.setText(challengeMode ? "Samostalna igra"
                : getString(stealPhase ? R.string.steal_turn_value : R.string.player_turn_value,
                stealPhase ? otherPlayer() : activePlayer()));
        renderHints();
        renderPoints();
        renderScore();
        renderTimer();
    }

    private void renderHints() {
        hintsContainer.removeAllViews();
        for (int i = 0; i < StepByStepEngine.HINT_COUNT; i++) {
            TextView hintView = new TextView(this);
            hintView.setTextSize(16);
            hintView.setTextColor(getResources().getColor(R.color.text_primary));
            hintView.setPadding(16, 12, 16, 12);
            hintView.setText(i < openedHints
                    ? getString(R.string.numbered_hint, i + 1, puzzle.hints[i])
                    : getString(R.string.hidden_hint, i + 1));
            hintView.setBackgroundResource(i < openedHints
                    ? R.drawable.card_background : R.drawable.input_background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 10);
            hintsContainer.addView(hintView, params);
        }
    }

    private void renderPoints() {
        pointsView.setText(stealPhase
                ? getString(R.string.possible_points, 5)
                : getString(R.string.possible_points, StepByStepEngine.pointsForHint(openedHints)));
    }

    private void renderScore() {
        scoreView.setText(challengeMode ? "Bodovi: " + playerOneScore
                : getString(R.string.two_player_score, playerOneScore, playerTwoScore));
    }

    private void renderTimer() {
        long seconds = (long) Math.ceil(remainingMillis / 1000.0);
        timerView.setText(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("roundIndex", roundIndex);
        outState.putInt("openedHints", openedHints);
        outState.putInt("playerOneScore", playerOneScore);
        outState.putInt("playerTwoScore", playerTwoScore);
        outState.putBoolean("stealPhase", stealPhase);
        outState.putLong("remainingMillis", remainingMillis);
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        super.onDestroy();
    }
}
