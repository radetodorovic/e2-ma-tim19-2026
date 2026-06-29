package com.example.mobilnekt1.match.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class StarTokenProgressTest {
    @Test
    public void awardsTokenForEveryFiftyPositivelyEarnedStars() {
        StarTokenProgress.Result result = StarTokenProgress.apply(45, 13);
        assertEquals(1, result.earnedTokens);
        assertEquals(8, result.remainingProgress);
    }

    @Test
    public void lossesDoNotReduceOrIncreaseProgress() {
        StarTokenProgress.Result result = StarTokenProgress.apply(20, -8);
        assertEquals(0, result.earnedTokens);
        assertEquals(20, result.remainingProgress);
    }
}
