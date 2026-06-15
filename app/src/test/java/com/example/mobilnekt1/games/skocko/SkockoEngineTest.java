package com.example.mobilnekt1.games.skocko;

import static org.junit.Assert.assertEquals;

import com.example.mobilnekt1.games.skocko.domain.SkockoEngine;
import org.junit.Test;

public class SkockoEngineTest {
    @Test public void repeatedSymbolsAreCountedOnce() {
        SkockoEngine.Result result = SkockoEngine.evaluate(
                new int[]{1, 1, 2, 3}, new int[]{1, 2, 1, 1});
        assertEquals(1, result.exact);
        assertEquals(2, result.misplaced);
    }

    @Test public void pointsDependOnAttemptPair() {
        assertEquals(20, SkockoEngine.pointsForAttempt(2));
        assertEquals(15, SkockoEngine.pointsForAttempt(4));
        assertEquals(10, SkockoEngine.pointsForAttempt(6));
    }
}
