package com.example.mobilnekt1.games.mynumber;

import java.util.Random;

public final class MyNumberGenerator {
    private static final int[] MEDIUM = {10, 15, 20};
    private static final int[] LARGE = {25, 50, 75, 100};

    private final Random random;

    public MyNumberGenerator() {
        this(new Random());
    }

    public MyNumberGenerator(long seed) {
        this(new Random(seed));
    }

    MyNumberGenerator(Random random) {
        this.random = random;
    }

    public MyNumberRound generate() {
        int[] numbers = new int[6];
        for (int i = 0; i < 4; i++) {
            numbers[i] = 1 + random.nextInt(9);
        }
        numbers[4] = MEDIUM[random.nextInt(MEDIUM.length)];
        numbers[5] = LARGE[random.nextInt(LARGE.length)];
        int target = 100 + random.nextInt(900);
        return new MyNumberRound(target, numbers);
    }
}
