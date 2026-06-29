package com.example.mobilnekt1.match.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.match.domain.MatchRewardCalculator;
import com.example.mobilnekt1.match.domain.MatchGameSequence;
import com.example.mobilnekt1.match.domain.MatchType;
import com.example.mobilnekt1.match.domain.StarTokenProgress;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MatchCompletionRepository {
    private final FirebaseProvider firebase;

    public MatchCompletionRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
    }

    public void finishMatch(String matchId, GameActionCallback callback) {
        settle(matchId, null, callback);
    }

    public void abandonMatch(String matchId, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla.");
            return;
        }
        markAbandoned(matchId, user.getUid(), callback);
    }

    private void markAbandoned(String matchId, String userId, GameActionCallback callback) {
        DocumentReference matchRef = firebase.getFirestore().collection("matches").document(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            if (!match.exists()) throw new IllegalStateException("Partija ne postoji.");
            String player1Id = match.getString("player1Id");
            String player2Id = match.getString("player2Id");
            if (!userId.equals(player1Id) && !userId.equals(player2Id)) {
                throw new IllegalStateException("Niste ucesnik ove partije.");
            }
            String status = match.getString("status");
            if ("finished".equals(status) || "abandoned".equals(status)
                    || match.getString("abandonedByUserId") != null) return null;
            if ("waiting".equals(status)) {
                transaction.update(matchRef, terminalMatchValues(
                        "abandoned", null, null, userId, 0, 0, 0, 0));
                return null;
            }
            if (!"active".equals(status)) throw new IllegalStateException("Partija nije aktivna.");

            DocumentReference userRef = firebase.getFirestore().collection("users").document(userId);
            DocumentSnapshot user = transaction.get(userRef);
            String currentGame = match.getString("currentGame");
            DocumentReference gameRef = currentGame == null || "none".equals(currentGame)
                    ? null : matchRef.collection("games").document(currentGame);
            DocumentSnapshot game = gameRef == null ? null : transaction.get(gameRef);

            Map<String, Object> userValues = new HashMap<>();
            userValues.put("inGame", false);
            userValues.put("activeMatchId", null);
            userValues.put("lastSettledMatchId", matchId);
            userValues.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(userRef, userValues);

            Map<String, Object> matchValues = new HashMap<>();
            matchValues.put("abandonedByUserId", userId);
            matchValues.put("loserId", userId);
            matchValues.put(userId.equals(player1Id) ? "player1InGame" : "player2InGame", false);
            matchValues.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(matchRef, matchValues);
            if (game != null && game.exists()) {
                Map<String, Object> gameValues = new HashMap<>();
                gameValues.put("abandonedPlayerId", userId);
                if (MatchGameSequence.ASSOCIATIONS.equals(currentGame)
                        && userId.equals(game.getString("activePlayerId"))) {
                    gameValues.put("activePlayerId",
                            userId.equals(player1Id) ? player2Id : player1Id);
                    gameValues.put("turnStage", "open");
                } else if (MatchGameSequence.QUIZ.equals(currentGame)
                        || MatchGameSequence.MY_NUMBER.equals(currentGame)
                        || userId.equals(game.getString("activePlayerId"))) {
                    gameValues.put("deadlineMillis", 0);
                }
                gameValues.put("updatedAt", FieldValue.serverTimestamp());
                transaction.update(gameRef, gameValues);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void settle(String matchId, String abandonedByUserId,
                        GameActionCallback callback) {
        if (!firebase.isConfigured()) {
            callback.onError("Firebase nije konfigurisan.");
            return;
        }
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Sesija je istekla.");
            return;
        }
        DocumentReference matchRef = firebase.getFirestore()
                .collection("matches").document(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            if (!match.exists()) {
                throw new IllegalStateException("Partija ne postoji.");
            }
            String player1Id = match.getString("player1Id");
            String player2Id = match.getString("player2Id");
            if (!currentUser.getUid().equals(player1Id)
                    && !currentUser.getUid().equals(player2Id)) {
                throw new IllegalStateException("Niste ucesnik ove partije.");
            }
            String status = match.getString("status");
            if ("finished".equals(status) || "abandoned".equals(status)) {
                return SettlementResult.alreadyApplied();
            }
            String effectiveAbandonedBy = abandonedByUserId == null
                    ? match.getString("abandonedByUserId") : abandonedByUserId;
            if (!hasAllGames(match.get("completedGames"))) {
                throw new IllegalStateException("Svih sest igara mora biti zavrseno.");
            }
            if (!"active".equals(status) || player2Id == null) {
                throw new IllegalStateException("Partija nije aktivna.");
            }

            DocumentReference player1Ref = firebase.getFirestore()
                    .collection("users").document(player1Id);
            DocumentReference player2Ref = firebase.getFirestore()
                    .collection("users").document(player2Id);
            DocumentSnapshot player1 = transaction.get(player1Ref);
            DocumentSnapshot player2 = transaction.get(player2Ref);
            if (!player1.exists() || !player2.exists()) {
                throw new IllegalStateException("Korisnicki profil jednog igraca ne postoji.");
            }

            long player1Score = value(match.getLong("player1Score"));
            long player2Score = value(match.getLong("player2Score"));
            String matchType = match.getString("matchType");
            if (!MatchType.isSupported(matchType)) {
                matchType = MatchType.REGULAR;
            }
            MatchRewardCalculator.Result reward = MatchRewardCalculator.calculate(
                    player1Id, player2Id, player1Score, player2Score,
                    matchType, effectiveAbandonedBy);

            DocumentReference player1StatsRef = firebase.getFirestore()
                    .collection("playerStats").document(player1Id);
            DocumentReference player2StatsRef = firebase.getFirestore()
                    .collection("playerStats").document(player2Id);
            DocumentSnapshot player1Stats = null;
            DocumentSnapshot player2Stats = null;
            if (MatchType.REGULAR.equals(matchType)) {
                player1Stats = transaction.get(player1StatsRef);
                player2Stats = transaction.get(player2StatsRef);
            }

            UserSettlement player1Settlement = settledUserValues(
                    player1, reward.player1StarDelta, matchId);
            UserSettlement player2Settlement = settledUserValues(
                    player2, reward.player2StarDelta, matchId);

            if (!player1Id.equals(effectiveAbandonedBy)) {
                transaction.update(player1Ref, player1Settlement.values);
            }
            if (!player2Id.equals(effectiveAbandonedBy)) {
                transaction.update(player2Ref, player2Settlement.values);
            }
            if (MatchType.REGULAR.equals(matchType)) {
                transaction.set(player1StatsRef, settledStatsValues(
                        player1Stats, player1Id, reward), SetOptions.merge());
                transaction.set(player2StatsRef, settledStatsValues(
                        player2Stats, player2Id, reward), SetOptions.merge());
            }
            transaction.update(matchRef, terminalMatchValues(
                    effectiveAbandonedBy == null ? "finished" : "abandoned",
                    reward.winnerId, reward.loserId, effectiveAbandonedBy,
                    reward.player1StarDelta, reward.player2StarDelta,
                    player1Settlement.tokenReward, player2Settlement.tokenReward));
            return SettlementResult.applied(reward.winnerId, matchType,
                    effectiveAbandonedBy != null);
        }).addOnSuccessListener(result -> {
            if (result.applied) {
                runPostMatchHooks(matchId, result);
            }
            callback.onSuccess();
        }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    private UserSettlement settledUserValues(DocumentSnapshot user, long starDelta,
                                              String matchId) {
        Map<String, Object> values = new HashMap<>();
        StarTokenProgress.Result tokenProgress = StarTokenProgress.apply(
                value(user.getLong("starTokenProgress")), starDelta);
        values.put("stars", nonNegative(value(user.getLong("stars")) + starDelta));
        values.put("weeklyStars", nonNegative(value(user.getLong("weeklyStars")) + starDelta));
        values.put("monthlyStars", nonNegative(value(user.getLong("monthlyStars")) + starDelta));
        values.put("tokens", value(user.getLong("tokens")) + tokenProgress.earnedTokens);
        values.put("starTokenProgress", tokenProgress.remainingProgress);
        values.put("inGame", false);
        values.put("activeMatchId", null);
        values.put("lastSettledMatchId", matchId);
        values.put("updatedAt", FieldValue.serverTimestamp());
        return new UserSettlement(values, tokenProgress.earnedTokens);
    }

    private Map<String, Object> settledStatsValues(DocumentSnapshot stats, String userId,
                                                    MatchRewardCalculator.Result reward) {
        Map<String, Object> values = new HashMap<>();
        values.put("totalMatches", value(stats == null ? null : stats.getLong("totalMatches")) + 1);
        long wins = value(stats == null ? null : stats.getLong("wins"));
        long losses = value(stats == null ? null : stats.getLong("losses"));
        if (userId.equals(reward.winnerId)) wins++;
        if (userId.equals(reward.loserId)) losses++;
        values.put("wins", wins);
        values.put("losses", losses);
        return values;
    }

    private Map<String, Object> terminalMatchValues(String status, String winnerId,
                                                    String loserId, String abandonedByUserId,
                                                    long player1StarDelta,
                                                    long player2StarDelta,
                                                    long player1TokenReward,
                                                    long player2TokenReward) {
        Map<String, Object> values = new HashMap<>();
        values.put("status", status);
        values.put("winnerId", winnerId);
        values.put("loserId", loserId);
        values.put("abandonedByUserId", abandonedByUserId);
        values.put("player1StarDelta", player1StarDelta);
        values.put("player2StarDelta", player2StarDelta);
        values.put("player1TokenReward", player1TokenReward);
        values.put("player2TokenReward", player2TokenReward);
        values.put("player1InGame", false);
        values.put("player2InGame", false);
        values.put("settlementApplied", true);
        values.put("currentGame", "none");
        values.put("currentTurnPlayerId", null);
        values.put("finishedAt", FieldValue.serverTimestamp());
        values.put("updatedAt", FieldValue.serverTimestamp());
        return values;
    }

    private void runPostMatchHooks(String matchId, SettlementResult result) {
        // KO integration points: updateLeagueForUser and updateRanking for both players.
        // Daily missions: WIN_MATCH for result.winnerId on regular matches, and
        // PLAY_FRIENDLY_MATCH for both players on friendly matches.
        // Notifications: match completion, abandonment and future league changes.
        // Per-match totals/wins/losses are already committed atomically above.
    }

    private boolean hasAllGames(Object value) {
        return value instanceof List && ((List<?>) value).size() >= MatchScoreRepository.REQUIRED_GAMES;
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private long nonNegative(long value) {
        return Math.max(0, value);
    }

    private String message(Exception error) {
        String message = error.getLocalizedMessage();
        return message == null ? "Partiju nije moguce zavrsiti." : message;
    }

    private static final class SettlementResult {
        final boolean applied;
        final String winnerId;
        final String matchType;
        final boolean abandoned;

        private SettlementResult(boolean applied, String winnerId,
                                 String matchType, boolean abandoned) {
            this.applied = applied;
            this.winnerId = winnerId;
            this.matchType = matchType;
            this.abandoned = abandoned;
        }

        static SettlementResult alreadyApplied() {
            return new SettlementResult(false, null, null, false);
        }

        static SettlementResult applied(String winnerId, String matchType, boolean abandoned) {
            return new SettlementResult(true, winnerId, matchType, abandoned);
        }
    }

    private static final class UserSettlement {
        final Map<String, Object> values;
        final long tokenReward;

        UserSettlement(Map<String, Object> values, long tokenReward) {
            this.values = values;
            this.tokenReward = tokenReward;
        }
    }
}
