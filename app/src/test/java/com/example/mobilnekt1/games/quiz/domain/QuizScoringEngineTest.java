package com.example.mobilnekt1.games.quiz.domain;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class QuizScoringEngineTest {
    @Test public void bothCorrect_awardsOnlyFasterPlayer() {
        QuizScoringEngine.Result result = QuizScoringEngine.score(2, 100L, 2, 150L, 2);
        assertEquals(10, result.player1Points);
        assertEquals(0, result.player2Points);
    }
    @Test public void wrongAnswerLosesFive_unansweredStaysZero() {
        QuizScoringEngine.Result result = QuizScoringEngine.score(1, 100L, null, null, 2);
        assertEquals(-5, result.player1Points);
        assertEquals(0, result.player2Points);
    }
}
