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
        commit(matchId, "koZnaZna", false, callback);
    }
    public void commitConnections(String matchId, GameActionCallback callback) {
        commit(matchId, "spojnice", true, callback);
    }

    @SuppressWarnings("unchecked")
    private void commit(String matchId, String gameId, boolean closesDemoMatch,
                        GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference gameRef = firebase.getFirestore().collection("matches").document(matchId)
                .collection("games").document(gameId);
        DocumentReference statsRef = firebase.getFirestore().collection("playerStats").document(user.getUid());
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot game = transaction.get(gameRef);
            if (!game.exists() || !"finished".equals(game.getString(gameId.equals("koZnaZna") ? "status" : "phase"))) return null;
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
            } else {
                stats.put("spojniceCorrectPairs", longValue(current.getLong("spojniceCorrectPairs"))
                        + longValue(game.getLong(player1 ? "player1CorrectPairs" : "player2CorrectPairs")));
                stats.put("spojniceTotalPairs", longValue(current.getLong("spojniceTotalPairs"))
                        + longValue(game.getLong(player1 ? "player1AttemptedPairs" : "player2AttemptedPairs")));
            }
            if (closesDemoMatch) {
                stats.put("totalMatches", longValue(current.getLong("totalMatches")) + 1);
                long p1 = longValue(game.getLong("player1Score"));
                long p2 = longValue(game.getLong("player2Score"));
                if ((player1 && p1 > p2) || (!player1 && p2 > p1))
                    stats.put("wins", longValue(current.getLong("wins")) + 1);
                else if (p1 != p2) stats.put("losses", longValue(current.getLong("losses")) + 1);
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
