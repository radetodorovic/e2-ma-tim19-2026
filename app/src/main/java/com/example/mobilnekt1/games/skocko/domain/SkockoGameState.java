package com.example.mobilnekt1.games.skocko.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class SkockoGameState {
    public String player1Id;
    public String player2Id;
    public int round;
    public String phase;
    public String startingPlayerId;
    public String activePlayerId;
    public long deadlineMillis;
    public List<Long> solution = new ArrayList<>();
    public int attempt;
    public List<String> history = new ArrayList<>();
    public long player1Score;
    public long player2Score;
    public Map<String, Long> player1SolvedAttempts = new HashMap<>();
    public Map<String, Long> player2SolvedAttempts = new HashMap<>();

    public SkockoGameState() { }
    public boolean isFinished() { return "finished".equals(phase); }
}
