package com.example.mobilnekt1.games.stepbystep.multiplayer;

import java.util.ArrayList;
import java.util.List;

public final class StepGameState {
    public int round;
    public int puzzleIndex;
    public String solution;
    public List<String> hints = new ArrayList<>();
    public String phase;
    public String player1Id;
    public String player2Id;
    public String activePlayerId;
    public long deadlineMillis;
    public long player1Score;
    public long player2Score;
    public long eventVersion;
    public String eventType;
    public String eventPlayerId;
    public long eventPoints;
    public int player1SolvedStep;
    public int player2SolvedStep;

    public StepGameState() {
    }

    public boolean isFinished() {
        return "finished".equals(phase);
    }
}
