package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.content.GameContentRepository;
import com.example.mobilnekt1.games.quiz.domain.QuizQuestion;
import java.util.List;

public class QuizActivity extends BaseKt1Activity {
    private int questionIndex = 0;
    private int playerScore = 0;
    private int opponentScore = 10;
    private boolean answerLocked = false;
    private TextView progressView;
    private TextView scoreView;
    private TextView questionView;
    private TextView timerView;
    private Button[] answerButtons;
    private boolean challengeMode;
    private CountDownTimer timer;
    private List<QuizQuestion> questions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        challengeMode = getIntent().getBooleanExtra(EXTRA_CHALLENGE_GAME, false);
        setContentView(R.layout.activity_quiz);

        progressView = findViewById(R.id.text_quiz_progress);
        scoreView = findViewById(R.id.text_quiz_score);
        questionView = findViewById(R.id.text_quiz_question);
        timerView = findViewById(R.id.text_quiz_timer);
        answerButtons = new Button[]{
                findViewById(R.id.button_answer_1),
                findViewById(R.id.button_answer_2),
                findViewById(R.id.button_answer_3),
                findViewById(R.id.button_answer_4)
        };

        for (int i = 0; i < answerButtons.length; i++) {
            int index = i;
            answerButtons[i].setOnClickListener(v -> answerQuestion(index));
        }
        setAnswersEnabled(false);
        new GameContentRepository(FirebaseProvider.getInstance(this).getFirestore())
                .loadQuizQuestions(new GameContentRepository.Callback<List<QuizQuestion>>() {
                    @Override public void onSuccess(List<QuizQuestion> value) {
                        questions = value; renderQuestion();
                    }
                    @Override public void onError(String message) {
                        showFinishDialog(getString(R.string.quiz_title), message);
                    }
                });
    }

    private void answerQuestion(int index) {
        if (answerLocked) {
            return;
        }
        answerLocked = true;
        if (timer != null) timer.cancel();
        QuizQuestion question = questions.get(questionIndex);
        playerScore += index == question.correctIndex ? 10 : -5;
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setEnabled(false);
            answerButtons[i].setBackgroundResource(i == index
                    ? R.drawable.selected_background
                    : R.drawable.button_outline);
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::nextQuestion, 600);
    }

    private void nextQuestion() {
        questionIndex++;
        if (questionIndex >= questions.size()) {
            String result = challengeMode ? "Ko zna zna je zavrsen.\nVasi bodovi: " + playerScore
                    : "Ko zna zna je zavrsen.\nIgrac 1: " + playerScore + "\nIgrac 2: " + opponentScore;
            showGameFinishDialog(getString(R.string.round_result), result, playerScore);
            return;
        }
        answerLocked = false;
        renderQuestion();
    }

    private void renderQuestion() {
        QuizQuestion question = questions.get(questionIndex);
        progressView.setText("Pitanje " + (questionIndex + 1) + "/" + questions.size());
        scoreView.setText(challengeMode ? "Bodovi: " + playerScore
                : "Igrac 1: " + playerScore + "  |  Igrac 2: " + opponentScore);
        questionView.setText(question.text);
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(question.answers.get(i));
            answerButtons[i].setEnabled(true);
            answerButtons[i].setBackgroundResource(R.drawable.button_outline);
        }
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(5_000, 250) {
            @Override public void onTick(long left) { timerView.setText("Timer: 00:0" + (int)Math.ceil(left / 1000.0)); }
            @Override public void onFinish() { if (!answerLocked) { answerLocked = true; nextQuestion(); } }
        }.start();
    }
    private void setAnswersEnabled(boolean enabled) {
        for (Button button : answerButtons) button.setEnabled(enabled);
    }
    @Override protected void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
