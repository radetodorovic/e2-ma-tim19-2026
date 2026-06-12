package com.example.mobilnekt1.games.mynumber;

import java.util.Arrays;

public final class MyNumberRound {
    private final int target;
    private final int[] numbers;

    public MyNumberRound(int target, int[] numbers) {
        this.target = target;
        this.numbers = Arrays.copyOf(numbers, numbers.length);
    }

    public int getTarget() {
        return target;
    }

    public int[] getNumbers() {
        return Arrays.copyOf(numbers, numbers.length);
    }
}
