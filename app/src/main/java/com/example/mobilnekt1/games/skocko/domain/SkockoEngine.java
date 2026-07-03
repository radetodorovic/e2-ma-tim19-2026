package com.example.mobilnekt1.games.skocko.domain;

public final class SkockoEngine {
    public static final String[] SYMBOLS = {"Skocko", "Kvadrat", "Krug", "Srce", "Trougao", "Zvezda"};
    private SkockoEngine() { }

    public static Result evaluate(int[] solution, int[] guess) {
        if (solution.length != 4 || guess.length != 4) {
            throw new IllegalArgumentException("Kombinacije moraju imati cetiri znaka.");
        }
        boolean[] usedSolution = new boolean[4];
        boolean[] usedGuess = new boolean[4];
        int exact = 0;
        int misplaced = 0;
        for (int i = 0; i < 4; i++) {
            if (solution[i] == guess[i]) {
                exact++;
                usedSolution[i] = true;
                usedGuess[i] = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (usedGuess[i]) continue;
            for (int j = 0; j < 4; j++) {
                if (!usedSolution[j] && guess[i] == solution[j]) {
                    misplaced++;
                    usedSolution[j] = true;
                    break;
                }
            }
        }
        return new Result(exact, misplaced);
    }

    public static int pointsForAttempt(int attemptNumber) {
        if (attemptNumber < 1 || attemptNumber > 6) return 0;
        if (attemptNumber <= 2) return 20;
        if (attemptNumber <= 4) return 15;
        return 10;
    }

    public static int[] solutionForSeed(String seed) {
        java.util.Random random = new java.util.Random(seed == null ? System.nanoTime() : seed.hashCode());
        int[] solution = new int[4];
        for (int i = 0; i < solution.length; i++) solution[i] = random.nextInt(SYMBOLS.length);
        return solution;
    }

    public static final class Result {
        public final int exact;
        public final int misplaced;

        public Result(int exact, int misplaced) {
            this.exact = exact;
            this.misplaced = misplaced;
        }
    }
}
