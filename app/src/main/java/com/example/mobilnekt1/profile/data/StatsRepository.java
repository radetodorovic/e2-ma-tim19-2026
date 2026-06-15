package com.example.mobilnekt1.profile.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatsRepository {
    private final FirebaseProvider firebase;
    public StatsRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void commitQuiz(String matchId, GameActionCallback callback) {
        commit(matchId, "koZnaZna", "status", callback);
    }
    public void commitConnections(String matchId, GameActionCallback callback) {
        commit(matchId, "spojnice", "phase", callback);
    }

    public void commitMyNumber(String matchId, GameActionCallback callback) {
        commit(matchId, "myNumber", "phase", callback);
    }

    public void commitStepByStep(String matchId, GameActionCallback callback) {
        commit(matchId, "stepByStep", "phase", callback);
    }

    public void commitAssociations(String matchId, GameActionCallback callback) {
        commit(matchId, "associations", "phase", callback);
    }

    public void commitSkocko(String matchId, GameActionCallback callback) {
        commit(matchId, "skocko", "phase", callback);
    }

    @SuppressWarnings("unchecked")
    private void commit(String matchId, String gameId, String statusField,
                        GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference gameRef = firebase.getFirestore().collection("matches").document(matchId)
                .collection("games").document(gameId);
        DocumentReference statsRef = firebase.getFirestore().collection("playerStats").document(user.getUid());
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot game = transaction.get(gameRef);
            if (!game.exists() || !"finished".equals(game.getString(statusField))) return null;
            String player1Id = game.getString("player1Id");
            boolean player1 = user.getUid().equals(player1Id);
            if (!player1 && !user.getUid().equals(game.getString("player2Id")))
                throw new IllegalStateException("Niste ucesnik igre.");
            List<String> committed = new ArrayList<>();
            Object committedValue = game.get("statsCommittedUids");
            if (committedValue instanceof List) for (Object value : (List<?>) committedValue) committed.add(String.valueOf(value));
            if (committed.contains(user.getUid())) return null;

            DocumentSnapshot current = transaction.get(statsRef);
            Map<String, Object> stats = current.exists() ? new HashMap<>(current.getData()) : new HashMap<>();
            Map<String, Number> averages = stats.get("averageScoreByGame") instanceof Map
                    ? new HashMap<>((Map<String, Number>) stats.get("averageScoreByGame")) : new HashMap<>();
            Map<String, Number> counts = stats.get("gamesPlayedByGame") instanceof Map
                    ? new HashMap<>((Map<String, Number>) stats.get("gamesPlayedByGame")) : new HashMap<>();
            long score = longValue(game.getLong(player1 ? "player1Score" : "player2Score"));
            long count = counts.containsKey(gameId) ? counts.get(gameId).longValue() : 0;
            double average = averages.containsKey(gameId) ? averages.get(gameId).doubleValue() : 0;
            averages.put(gameId, (average * count + score) / (count + 1));
            counts.put(gameId, count + 1);
            stats.put("averageScoreByGame", averages);
            stats.put("gamesPlayedByGame", counts);
            if (gameId.equals("koZnaZna")) {
                stats.put("koZnaZnaCorrect", longValue(current.getLong("koZnaZnaCorrect"))
                        + longValue(game.getLong(player1 ? "player1Correct" : "player2Correct")));
                stats.put("koZnaZnaWrong", longValue(current.getLong("koZnaZnaWrong"))
                        + longValue(game.getLong(player1 ? "player1Wrong" : "player2Wrong")));
            } else if (gameId.equals("spojnice")) {
                stats.put("spojniceCorrectPairs", longValue(current.getLong("spojniceCorrectPairs"))
                        + longValue(game.getLong(player1 ? "player1CorrectPairs" : "player2CorrectPairs")));
                stats.put("spojniceTotalPairs", longValue(current.getLong("spojniceTotalPairs"))
                        + longValue(game.getLong(player1 ? "player1AttemptedPairs" : "player2AttemptedPairs")));
            } else if (gameId.equals("myNumber")) {
                stats.put("myNumberExactRounds", longValue(current.getLong("myNumberExactRounds"))
                        + longValue(game.getLong(player1 ? "player1ExactRounds" : "player2ExactRounds")));
                stats.put("myNumberTotalRounds", longValue(current.getLong("myNumberTotalRounds")) + 2);
            } else if (gameId.equals("stepByStep")) {
                Map<String, Number> solvedByHint = stats.get("stepSolvedByHint") instanceof Map
                        ? new HashMap<>((Map<String, Number>) stats.get("stepSolvedByHint")) : new HashMap<>();
                int solvedStep = (int) longValue(game.getLong(player1
                        ? "player1SolvedStep" : "player2SolvedStep"));
                if (solvedStep > 0) {
                    String key = String.valueOf(solvedStep);
                    long solvedCount = solvedByHint.containsKey(key)
                            ? solvedByHint.get(key).longValue() : 0;
                    solvedByHint.put(key, solvedCount + 1);
                }
                stats.put("stepSolvedByHint", solvedByHint);
                stats.put("stepRoundsPlayed", longValue(current.getLong("stepRoundsPlayed")) + 1);
            } else if (gameId.equals("associations")) {
                stats.put("associationsSolved", longValue(current.getLong("associationsSolved"))
                        + longValue(game.getLong(player1 ? "player1SolvedRounds" : "player2SolvedRounds")));
                stats.put("associationsTotal", longValue(current.getLong("associationsTotal")) + 2);
            } else if (gameId.equals("skocko")) {
                Map<String, Number> solvedAttempts = stats.get("skockoSolvedByAttempt") instanceof Map
                        ? new HashMap<>((Map<String, Number>) stats.get("skockoSolvedByAttempt")) : new HashMap<>();
                Object gameAttemptsValue = game.get(player1 ? "player1SolvedAttempts" : "player2SolvedAttempts");
                if (gameAttemptsValue instanceof Map) {
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) gameAttemptsValue).entrySet()) {
                        long previous = solvedAttempts.containsKey(entry.getKey())
                                ? solvedAttempts.get(entry.getKey()).longValue() : 0;
                        solvedAttempts.put(entry.getKey(), previous + ((Number) entry.getValue()).longValue());
                    }
                }
                stats.put("skockoSolvedByAttempt", solvedAttempts);
                stats.put("skockoRoundsPlayed", longValue(current.getLong("skockoRoundsPlayed")) + 1);
            }
            transaction.set(statsRef, stats);
            committed.add(user.getUid());
            transaction.update(gameRef, "statsCommittedUids", committed);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(error.getLocalizedMessage()));
    }
    private long longValue(Long value) { return value == null ? 0 : value; }
}
