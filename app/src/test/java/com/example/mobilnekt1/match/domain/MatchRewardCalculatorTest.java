package com.example.mobilnekt1.match.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class MatchRewardCalculatorTest {
    @Test
    public void regularMatch_appliesSpecificationExample() {
        MatchRewardCalculator.Result result = MatchRewardCalculator.calculate(
                "A", "B", 150, 100, MatchType.REGULAR, null);

        assertEquals("A", result.winnerId);
        assertEquals("B", result.loserId);
        assertEquals(13, result.player1StarDelta);
        assertEquals(-8, result.player2StarDelta);
    }

    @Test
    public void friendlyMatch_hasWinnerButNoStarChanges() {
        MatchRewardCalculator.Result result = MatchRewardCalculator.calculate(
                "A", "B", 80, 40, MatchType.FRIENDLY, null);

        assertEquals("A", result.winnerId);
        assertEquals(0, result.player1StarDelta);
        assertEquals(0, result.player2StarDelta);
    }

    @Test
    public void abandonedMatch_givesAbandoningPlayerNoStars() {
        MatchRewardCalculator.Result result = MatchRewardCalculator.calculate(
                "A", "B", 40, 80, MatchType.REGULAR, "B");

        assertEquals("A", result.winnerId);
        assertEquals("B", result.loserId);
        assertEquals(11, result.player1StarDelta);
        assertEquals(0, result.player2StarDelta);
    }

    @Test
    public void draw_hasNoWinnerAndNoStarChanges() {
        MatchRewardCalculator.Result result = MatchRewardCalculator.calculate(
                "A", "B", 100, 100, MatchType.REGULAR, null);

        assertNull(result.winnerId);
        assertNull(result.loserId);
        assertEquals(0, result.player1StarDelta);
        assertEquals(0, result.player2StarDelta);
    }
}
