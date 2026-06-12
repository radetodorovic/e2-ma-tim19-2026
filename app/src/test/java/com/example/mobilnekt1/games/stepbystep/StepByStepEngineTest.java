package com.example.mobilnekt1.games.stepbystep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StepByStepEngineTest {
    @Test
    public void pointsDecreaseByTwoForEachHint() {
        assertEquals(20, StepByStepEngine.pointsForHint(1));
        assertEquals(12, StepByStepEngine.pointsForHint(5));
        assertEquals(8, StepByStepEngine.pointsForHint(7));
    }

    @Test
    public void answerComparisonIgnoresCaseAndWhitespace() {
        assertTrue(StepByStepEngine.matches("  android ", "Android"));
    }
}
