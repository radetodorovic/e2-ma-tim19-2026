package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        renderQuestion();
    }

    private void answerQuestion(int index) {
        if (answerLocked) {
            return;
        }
        answerLocked = true;
        MockStudentTwoData.QuizQuestion question = MockStudentTwoData.QUIZ_QUESTIONS[questionIndex];
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
        if (questionIndex >= MockStudentTwoData.QUIZ_QUESTIONS.length) {
            showFinishDialog(getString(R.string.round_result),
                    "Ko zna zna je zavrsen u KT1 mock rezimu."
                            + "\nIgrac 1: " + playerScore
                            + "\nIgrac 2: " + opponentScore
                            + "\nPravilo brzeg igraca nije implementirano za KT1.");
            return;
        }
        answerLocked = false;
        renderQuestion();
    }

    private void renderQuestion() {
        MockStudentTwoData.QuizQuestion question = MockStudentTwoData.QUIZ_QUESTIONS[questionIndex];
        progressView.setText("Pitanje " + (questionIndex + 1) + "/5");
        scoreView.setText("Igrac 1: " + playerScore + "  |  Igrac 2: " + opponentScore);
        timerView.setText("Timer: 00:05");
        questionView.setText(question.question);
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(question.answers[i]);
            answerButtons[i].setEnabled(true);
            answerButtons[i].setBackgroundResource(R.drawable.button_outline);
        }
    }
}
