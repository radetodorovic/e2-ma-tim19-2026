package com.example.mobilnekt1.games.connections.domain;

import java.util.ArrayList;
import java.util.List;

public final class ConnectionsGameState {
    public String player1Id;
    public String player2Id;
    public int round;
    public String phase;
    public String startingPlayerId;
    public String activePlayerId;
    public long deadlineMillis;
    public List<String> leftItems = new ArrayList<>();
    public List<String> rightItems = new ArrayList<>();
    public List<Long> correctMatches = new ArrayList<>();
    public List<Long> solvedLeft = new ArrayList<>();
    public int currentLeft;
    public long player1Score;
    public long player2Score;
    public long player1CorrectPairs;
    public long player1AttemptedPairs;
    public long player2CorrectPairs;
    public long player2AttemptedPairs;

    public ConnectionsGameState() { }
    public boolean isFinished() { return "finished".equals(phase); }
    public boolean isSolved(int index) { return solvedLeft.contains((long) index); }
}
