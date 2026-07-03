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
    public String matchType;
    public String currentGame;
    public long currentGameVersion;
    public String currentTurnPlayerId;
    public String winnerId;
    public String loserId;
    public String abandonedByUserId;
    public long player1Score;
    public long player2Score;
    public long player1StarDelta;
    public long player2StarDelta;
    public long player1TokenReward;
    public long player2TokenReward;
    public boolean player1InGame;
    public boolean player2InGame;
    public boolean settlementApplied;
    public String tournamentId;
    public String tournamentStage;
    public List<String> completedGames = new ArrayList<>();
    public Timestamp createdAt;
    public Timestamp startedAt;
    public Timestamp finishedAt;
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

    public boolean isAbandoned() {
        return "abandoned".equals(status);
    }

    public boolean isTerminal() {
        return isFinished() || isAbandoned();
    }

    public boolean isGameCompleted(String gameId) {
        return completedGames != null && completedGames.contains(gameId);
    }
}
