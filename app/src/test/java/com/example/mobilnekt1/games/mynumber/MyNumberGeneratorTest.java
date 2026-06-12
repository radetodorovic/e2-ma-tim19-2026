package com.example.mobilnekt1.games.mynumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

public class MyNumberGeneratorTest {
    @Test
    public void generatedRoundFollowsRequiredNumberGroups() {
        MyNumberRound round = new MyNumberGenerator(new Random(42)).generate();
        int[] values = round.getNumbers();

        assertEquals(6, values.length);
        for (int i = 0; i < 4; i++) {
            assertTrue(values[i] >= 1 && values[i] <= 9);
        }
        assertTrue(values[4] == 10 || values[4] == 15 || values[4] == 20);
        assertTrue(values[5] == 25 || values[5] == 50 || values[5] == 75 || values[5] == 100);
        assertTrue(round.getTarget() >= 100 && round.getTarget() <= 999);
    }
}
