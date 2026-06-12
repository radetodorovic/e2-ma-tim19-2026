package com.example.mobilnekt1.games.stepbystep;

public final class StepByStepEngine {
    public static final int HINT_COUNT = 7;
    public static final int ROUND_SECONDS = 70;
    public static final int STEAL_SECONDS = 10;

    private StepByStepEngine() {
    }

    public static int pointsForHint(int openedHints) {
        if (openedHints < 1 || openedHints > HINT_COUNT) {
            throw new IllegalArgumentException("Broj otvorenih koraka mora biti od 1 do 7.");
        }
        return 20 - (openedHints - 1) * 2;
    }

    public static boolean matches(String answer, String solution) {
        return answer != null && solution != null
                && answer.trim().equalsIgnoreCase(solution.trim());
    }
}
