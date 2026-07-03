package com.example.mobilnekt1.games.mynumber;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class ExpressionTokenPolicyTest {
    @Test public void consecutiveOperators_areRejected() {
        assertFalse(ExpressionTokenPolicy.canAppend(Arrays.asList("5", "*"), "-"));
    }

    @Test public void expressionCannotStartWithOperator() {
        assertFalse(ExpressionTokenPolicy.canAppend(Collections.emptyList(), "+"));
    }

    @Test public void validOperatorAndNumberSequence_isAccepted() {
        assertTrue(ExpressionTokenPolicy.canAppend(Collections.singletonList("5"), "*"));
        assertTrue(ExpressionTokenPolicy.canAppend(Arrays.asList("5", "*"), "3"));
    }

    @Test public void closingParenthesis_requiresOpenParenthesisAndValue() {
        assertFalse(ExpressionTokenPolicy.canAppend(Collections.singletonList("5"), ")"));
        assertTrue(ExpressionTokenPolicy.canAppend(Arrays.asList("(", "5"), ")"));
    }

    @Test public void adjacentNumbers_areRejected() {
        assertFalse(ExpressionTokenPolicy.canAppend(Collections.singletonList("5"), "3"));
    }
}
