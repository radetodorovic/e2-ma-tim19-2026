package com.example.mobilnekt1.match.domain;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public final class Match {
    public String id;
    public String player1Id;
    public String player1Name;
    public String player2Id;
    public String player2Name;
    public String status;
    public String currentGame;
    public long currentGameVersion;
    public String currentTurnPlayerId;
    public String winnerId;
    public long player1Score;
    public long player2Score;
    public List<String> completedGames = new ArrayList<>();
    public Timestamp createdAt;
    public Timestamp updatedAt;

    public Match() {
    }

    public boolean isWaiting() {
        return "waiting".equals(status);
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public boolean isFinished() {
        return "finished".equals(status);
    }

    public boolean isGameCompleted(String gameId) {
        return completedGames != null && completedGames.contains(gameId);
    }
}
