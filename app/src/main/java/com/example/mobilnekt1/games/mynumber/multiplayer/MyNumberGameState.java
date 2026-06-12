package com.example.mobilnekt1.games.mynumber.multiplayer;

import java.util.ArrayList;
import java.util.List;

public final class MyNumberGameState {
    public int round;
    public String phase;
    public String player1Id;
    public String player2Id;
    public String startingPlayerId;
    public long deadlineMillis;
    public long target;
    public List<Long> numbers = new ArrayList<>();
    public Long player1Result;
    public Long player2Result;
    public boolean player1Submitted;
    public boolean player2Submitted;
    public long player1Score;
    public long player2Score;

    public MyNumberGameState() {
    }

    public int[] numberArray() {
        int[] result = new int[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            result[i] = numbers.get(i).intValue();
        }
        return result;
    }

    public boolean isFinished() {
        return "finished".equals(phase);
    }

    public boolean hasSubmitted(String uid) {
        return uid != null && (uid.equals(player1Id) ? player1Submitted : player2Submitted);
    }
}
