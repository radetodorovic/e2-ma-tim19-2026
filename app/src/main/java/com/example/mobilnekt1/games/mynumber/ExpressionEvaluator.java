package com.example.mobilnekt1.games.mynumber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExpressionEvaluator {
    public static final class Result {
        public final boolean valid;
        public final int value;
        public final String error;

        private Result(boolean valid, int value, String error) {
            this.valid = valid;
            this.value = value;
            this.error = error;
        }

        public static Result success(int value) {
            return new Result(true, value, null);
        }

        public static Result error(String message) {
            return new Result(false, 0, message);
        }
    }

    private ExpressionEvaluator() {
    }

    public static Result evaluate(String expression, int[] offeredNumbers) {
        try {
            Parser parser = new Parser(tokenize(expression), offeredNumbers);
            long value = parser.parseExpression();
            if (parser.hasMore()) {
                return Result.error("Izraz nije ispravan.");
            }
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                return Result.error("Rezultat je van dozvoljenog opsega.");
            }
            return Result.success((int) value);
        } catch (IllegalArgumentException | ArithmeticException error) {
            return Result.error(error.getMessage());
        }
    }

    private static List<String> tokenize(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Unesite izraz.");
        }
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < expression.length()) {
            char value = expression.charAt(index);
            if (Character.isWhitespace(value)) {
                index++;
            } else if (Character.isDigit(value)) {
                int start = index;
                while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
                    index++;
                }
                tokens.add(expression.substring(start, index));
            } else if ("()+-*/".indexOf(value) >= 0) {
                tokens.add(String.valueOf(value));
                index++;
            } else {
                throw new IllegalArgumentException("Izraz sadrzi nedozvoljen znak.");
            }
        }
        return tokens;
    }

    private static final class Parser {
        private final List<String> tokens;
        private final Map<Integer, Integer> available = new HashMap<>();
        private int position;

        private Parser(List<String> tokens, int[] offeredNumbers) {
            this.tokens = tokens;
            for (int number : offeredNumbers) {
                Integer current = available.get(number);
                available.put(number, (current == null ? 0 : current) + 1);
            }
        }

        private long parseExpression() {
            long value = parseTerm();
            while (peek("+") || peek("-")) {
                String operator = next();
                long right = parseTerm();
                value = operator.equals("+") ? Math.addExact(value, right) : Math.subtractExact(value, right);
            }
            return value;
        }

        private long parseTerm() {
            long value = parseFactor();
            while (peek("*") || peek("/")) {
                String operator = next();
                long right = parseFactor();
                if (operator.equals("*")) {
                    value = Math.multiplyExact(value, right);
                } else {
                    if (right == 0) {
                        throw new IllegalArgumentException("Deljenje nulom nije dozvoljeno.");
                    }
                    if (value % right != 0) {
                        throw new IllegalArgumentException("Deljenje mora dati ceo broj.");
                    }
                    value /= right;
                }
            }
            return value;
        }

        private long parseFactor() {
            if (peek("(")) {
                next();
                long value = parseExpression();
                if (!peek(")")) {
                    throw new IllegalArgumentException("Nedostaje zatvorena zagrada.");
                }
                next();
                return value;
            }
            if (!hasMore() || !tokens.get(position).matches("\\d+")) {
                throw new IllegalArgumentException("Ocekivan je ponudjeni broj.");
            }
            int number = Integer.parseInt(next());
            Integer storedCount = available.get(number);
            int count = storedCount == null ? 0 : storedCount;
            if (count == 0) {
                throw new IllegalArgumentException("Broj " + number + " nije dostupan ili je vec iskoriscen.");
            }
            available.put(number, count - 1);
            return number;
        }

        private boolean peek(String token) {
            return hasMore() && tokens.get(position).equals(token);
        }

        private String next() {
            return tokens.get(position++);
        }

        private boolean hasMore() {
            return position < tokens.size();
        }
    }
}
