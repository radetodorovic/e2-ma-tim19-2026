package com.example.mobilnekt1.games.quiz.domain;

public final class QuizScoringEngine {
    public static final class Result {
        public final int player1Points;
        public final int player2Points;
        public final boolean player1Correct;
        public final boolean player2Correct;
        Result(int p1, int p2, boolean c1, boolean c2) {
            player1Points = p1; player2Points = p2; player1Correct = c1; player2Correct = c2;
        }
    }
    private QuizScoringEngine() { }
    public static Result score(Integer p1Answer, Long p1Time, Integer p2Answer, Long p2Time,
                               int correctIndex) {
        boolean p1Correct = p1Answer != null && p1Answer == correctIndex;
        boolean p2Correct = p2Answer != null && p2Answer == correctIndex;
        int p1 = 0, p2 = 0;
        if (p1Correct && p2Correct) {
            if (p1Time != null && p2Time != null && p1Time <= p2Time) p1 = 10;
            else p2 = 10;
        } else {
            if (p1Correct) p1 = 10; else if (p1Answer != null) p1 = -5;
            if (p2Correct) p2 = 10; else if (p2Answer != null) p2 = -5;
        }
        return new Result(p1, p2, p1Correct, p2Correct);
    }
}
