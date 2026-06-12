package com.example.mobilnekt1.match.domain;

import com.google.firebase.Timestamp;

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
}
