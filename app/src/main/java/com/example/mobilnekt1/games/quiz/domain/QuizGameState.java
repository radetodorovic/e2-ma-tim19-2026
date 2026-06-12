package com.example.mobilnekt1.games.quiz.domain;

import java.util.ArrayList;
import java.util.List;

public final class QuizGameState {
    public String player1Id;
    public String player2Id;
    public String status;
    public int currentQuestionIndex;
    public long deadlineMillis;
    public List<QuizQuestion> questions = new ArrayList<>();
    public Long player1Answer;
    public Long player2Answer;
    public Long player1AnsweredAt;
    public Long player2AnsweredAt;
    public boolean player1Submitted;
    public boolean player2Submitted;
    public long player1Score;
    public long player2Score;
    public long player1Correct;
    public long player1Wrong;
    public long player2Correct;
    public long player2Wrong;

    public QuizGameState() {
    }

    public QuizQuestion currentQuestion() {
        if (currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) {
            return null;
        }
        return questions.get(currentQuestionIndex);
    }

    public boolean hasSubmitted(String uid) {
        return uid != null && (uid.equals(player1Id) ? player1Submitted : player2Submitted);
    }

    public boolean isFinished() {
        return "finished".equals(status);
    }
}
