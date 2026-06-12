package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.games.quiz.domain.QuizGameState;
import com.example.mobilnekt1.games.quiz.domain.QuizQuestion;
import com.example.mobilnekt1.games.quiz.presentation.QuizGameViewModel;

import java.util.Locale;

public final class MultiplayerQuizActivity extends BaseKt1Activity {
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private QuizGameViewModel viewModel;
    private QuizGameState state;
    private TextView progressView;
    private TextView timerView;
    private TextView scoreView;
    private TextView questionView;
    private Button[] answerButtons;
    private boolean resultShown;
    private boolean resolving;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        String matchId = getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID);
        if (matchId == null) { finish(); return; }
        bindViews();
        viewModel = new ViewModelProvider(this).get(QuizGameViewModel.class);
        viewModel.getState().observe(this, value -> { state = value; resolving = false; render(); });
        viewModel.getError().observe(this, event -> {
            String message = event.getIfNotHandled();
            if (message != null) showInfoDialog(getString(R.string.match_error_title), message);
        });
        for (int i = 0; i < answerButtons.length; i++) {
            int index = i;
            answerButtons[i].setOnClickListener(v -> {
                setAnswersEnabled(false);
                viewModel.answer(index);
            });
        }
        viewModel.start(matchId);
        ticker.post(tick);
    }

    private void bindViews() {
        progressView = findViewById(R.id.text_quiz_progress);
        timerView = findViewById(R.id.text_quiz_timer);
        scoreView = findViewById(R.id.text_quiz_score);
        questionView = findViewById(R.id.text_quiz_question);
        answerButtons = new Button[]{findViewById(R.id.button_answer_1),
                findViewById(R.id.button_answer_2), findViewById(R.id.button_answer_3),
                findViewById(R.id.button_answer_4)};
    }

    private void render() {
        if (state == null) return;
        QuizQuestion question = state.currentQuestion();
        progressView.setText(getString(R.string.quiz_progress_value,
                Math.min(state.currentQuestionIndex + 1, 5), 5));
        scoreView.setText(getString(R.string.two_player_score,
                (int) state.player1Score, (int) state.player2Score));
        if (question != null) {
            questionView.setText(question.text);
            for (int i = 0; i < answerButtons.length; i++) {
                answerButtons[i].setText(i < question.answers.size() ? question.answers.get(i) : "");
                answerButtons[i].setBackgroundResource(R.drawable.button_outline);
            }
        }
        setAnswersEnabled(!state.isFinished() && !state.hasSubmitted(viewModel.currentUserId()));
        if (state.hasSubmitted(viewModel.currentUserId()) && !state.isFinished()) {
            timerView.setText(R.string.waiting_for_opponent_answer);
        }
        if (state.isFinished() && !resultShown) {
            resultShown = true;
            showFinishDialog(getString(R.string.game_result), getString(R.string.two_player_score,
                    (int) state.player1Score, (int) state.player2Score));
        }
    }

    private void setAnswersEnabled(boolean enabled) {
        for (Button button : answerButtons) button.setEnabled(enabled);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (state != null && !state.isFinished()) {
                long remaining = Math.max(0, state.deadlineMillis - System.currentTimeMillis());
                long seconds = (long) Math.ceil(remaining / 1000.0);
                if (!state.hasSubmitted(viewModel.currentUserId())) {
                    timerView.setText(String.format(Locale.getDefault(), "00:%02d", seconds));
                }
                if (remaining == 0 && !resolving) {
                    resolving = true;
                    viewModel.resolveIfExpired();
                }
            }
            ticker.postDelayed(this, 200);
        }
    };

    @Override protected void onDestroy() {
        ticker.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
