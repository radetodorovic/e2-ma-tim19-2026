package com.example.mobilnekt1.games.mynumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExpressionEvaluatorTest {
    private final int[] numbers = {3, 7, 8, 9, 15, 75};

    @Test
    public void evaluatesPrecedenceAndParentheses() {
        ExpressionEvaluator.Result result = ExpressionEvaluator.evaluate("(75 - 15) * 7 + 9", numbers);
        assertTrue(result.valid);
        assertEquals(429, result.value);
    }

    @Test
    public void rejectsUnavailableOrRepeatedNumber() {
        assertFalse(ExpressionEvaluator.evaluate("3 + 3", numbers).valid);
        assertFalse(ExpressionEvaluator.evaluate("100 + 3", numbers).valid);
    }

    @Test
    public void rejectsFractionalDivisionAndDivisionByZero() {
        assertFalse(ExpressionEvaluator.evaluate("7 / 3", numbers).valid);
        assertFalse(ExpressionEvaluator.evaluate("3 / (8 - 8)", numbers).valid);
    }
}
