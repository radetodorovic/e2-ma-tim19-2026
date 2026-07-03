package com.example.mobilnekt1.games.mynumber;

import java.util.List;

public final class ExpressionTokenPolicy {
    private ExpressionTokenPolicy() { }

    public static boolean canAppend(List<String> tokens, String token) {
        if (token == null || token.isEmpty()) return false;
        String previous = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
        if (isNumber(token)) {
            return previous == null || isOperator(previous) || "(".equals(previous);
        }
        if ("(".equals(token)) {
            return previous == null || isOperator(previous) || "(".equals(previous);
        }
        if (")".equals(token)) {
            return previous != null && (isNumber(previous) || ")".equals(previous))
                    && unmatchedOpenParentheses(tokens) > 0;
        }
        if (isOperator(token)) {
            return previous != null && (isNumber(previous) || ")".equals(previous));
        }
        return false;
    }

    private static int unmatchedOpenParentheses(List<String> tokens) {
        int count = 0;
        for (String token : tokens) {
            if ("(".equals(token)) count++;
            else if (")".equals(token)) count--;
        }
        return count;
    }

    private static boolean isOperator(String token) {
        return "+".equals(token) || "-".equals(token)
                || "*".equals(token) || "/".equals(token);
    }

    private static boolean isNumber(String token) {
        if (token == null || token.isEmpty()) return false;
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) return false;
        }
        return true;
    }
}
