package com.example.mobilnekt1.games.skocko.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.games.skocko.domain.SkockoEngine;
import com.example.mobilnekt1.games.skocko.domain.SkockoGameState;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SkockoGameRepository {
    private static final long ROUND_MILLIS = 30_000L;
    private static final long STEAL_MILLIS = 10_000L;
    private final FirebaseProvider firebase;
    private final Random random = new Random();
    private ListenerRegistration registration;

    public SkockoGameRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }
    public String currentUserId() {
        FirebaseUser user = firebase.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void initialize(String matchId, GameActionCallback callback) {
        List<Integer> solution = generateSolution();
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            if (!transaction.get(gameRef).exists())
                transaction.set(gameRef, roundData(match, 0, match.getString("player1Id"), 0, 0,
                        solution, new HashMap<>(), new HashMap<>()));
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void listen(String matchId, SkockoGameListener listener) {
        stopListening();
        registration = gameRef(matchId).addSnapshotListener((snapshot, error) -> {
            if (error != null) listener.onError(message(error));
            else if (snapshot != null && snapshot.exists()) {
                SkockoGameState state = snapshot.toObject(SkockoGameState.class);
                if (state != null) listener.onChanged(state);
            }
        });
    }

    public void submitGuess(String matchId, int[] guess, GameActionCallback callback) {
        List<Integer> nextSolution = generateSolution();
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            DocumentSnapshot game = transaction.get(gameRef);
            if (!game.exists() || "finished".equals(game.getString("phase"))) return null;
            if (!currentUserId().equals(game.getString("activePlayerId")))
                throw new IllegalStateException("Protivnik je na potezu.");
            if (guess.length != 4) throw new IllegalStateException("Izaberite cetiri znaka.");
            int[] solution = intArray(game.get("solution"));
            SkockoEngine.Result result = SkockoEngine.evaluate(solution, guess);
            List<String> history = stringList(game.get("history"));
            history.add(encode(guess, result));
            String phase = game.getString("phase");
            int attempt = (int) value(game.getLong("attempt"));
            if (result.exact == 4) {
                int points = "steal".equals(phase) ? 10 : SkockoEngine.pointsForAttempt(attempt + 1);
                finishRound(transaction, match, game, gameRef, currentUserId(), points, history, nextSolution);
            } else if ("steal".equals(phase)) {
                finishRound(transaction, match, game, gameRef, null, 0, history, nextSolution);
            } else if (attempt >= 5) {
                transaction.update(gameRef, "phase", "steal", "activePlayerId", otherPlayer(match, currentUserId()),
                        "deadlineMillis", System.currentTimeMillis() + STEAL_MILLIS, "attempt", 0,
                        "history", history, "updatedAt", FieldValue.serverTimestamp());
            } else {
                transaction.update(gameRef, "attempt", attempt + 1, "history", history,
                        "updatedAt", FieldValue.serverTimestamp());
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void advanceExpired(String matchId, GameActionCallback callback) {
        List<Integer> nextSolution = generateSolution();
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            DocumentSnapshot game = transaction.get(gameRef);
            Long deadline = game.getLong("deadlineMillis");
            if (!game.exists() || "finished".equals(game.getString("phase")) || deadline == null
                    || deadline > System.currentTimeMillis()) return null;
            if ("playing".equals(game.getString("phase"))) {
                transaction.update(gameRef, "phase", "steal",
                        "activePlayerId", otherPlayer(match, game.getString("startingPlayerId")),
                        "deadlineMillis", System.currentTimeMillis() + STEAL_MILLIS, "attempt", 0,
                        "updatedAt", FieldValue.serverTimestamp());
            } else {
                finishRound(transaction, match, game, gameRef, null, 0,
                        stringList(game.get("history")), nextSolution);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void finishRound(Transaction transaction, DocumentSnapshot match, DocumentSnapshot game,
                             DocumentReference gameRef, String scoringPlayer, int points,
                             List<String> history, List<Integer> nextSolution) {
        long p1 = value(game.getLong("player1Score"));
        long p2 = value(game.getLong("player2Score"));
        Map<String, Long> p1Attempts = longMap(game.get("player1SolvedAttempts"));
        Map<String, Long> p2Attempts = longMap(game.get("player2SolvedAttempts"));
        if (scoringPlayer != null) {
            if (scoringPlayer.equals(match.getString("player1Id"))) p1 += points; else p2 += points;
            if (!"steal".equals(game.getString("phase"))) {
                String key = String.valueOf(value(game.getLong("attempt")) + 1);
                Map<String, Long> attempts = scoringPlayer.equals(match.getString("player1Id"))
                        ? p1Attempts : p2Attempts;
                long previous = attempts.containsKey(key) ? attempts.get(key) : 0L;
                attempts.put(key, previous + 1);
            }
        }
        int round = (int) value(game.getLong("round"));
        if (round >= 1) {
            transaction.update(gameRef, "player1Score", p1, "player2Score", p2,
                    "history", history, "phase", "finished", "deadlineMillis", 0,
                    "player1SolvedAttempts", p1Attempts, "player2SolvedAttempts", p2Attempts,
                    "updatedAt", FieldValue.serverTimestamp());
        } else {
            transaction.set(gameRef, roundData(match, 1, match.getString("player2Id"), p1, p2,
                    nextSolution, p1Attempts, p2Attempts));
        }
    }

    private Map<String, Object> roundData(DocumentSnapshot match, int round, String starter,
                                          long p1, long p2, List<Integer> solution,
                                          Map<String, Long> p1Attempts, Map<String, Long> p2Attempts) {
        Map<String, Object> data = new HashMap<>();
        data.put("player1Id", match.getString("player1Id"));
        data.put("player2Id", match.getString("player2Id"));
        data.put("round", round);
        data.put("phase", "playing");
        data.put("startingPlayerId", starter);
        data.put("activePlayerId", starter);
        data.put("deadlineMillis", System.currentTimeMillis() + ROUND_MILLIS);
        data.put("solution", solution);
        data.put("attempt", 0);
        data.put("history", new ArrayList<>());
        data.put("player1Score", p1);
        data.put("player2Score", p2);
        data.put("player1SolvedAttempts", p1Attempts);
        data.put("player2SolvedAttempts", p2Attempts);
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private List<Integer> generateSolution() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) result.add(random.nextInt(6));
        return result;
    }
    private String encode(int[] guess, SkockoEngine.Result result) {
        return guess[0] + "," + guess[1] + "," + guess[2] + "," + guess[3]
                + "|" + result.exact + "|" + result.misplaced;
    }
    @SuppressWarnings("unchecked") private int[] intArray(Object value) {
        List<Object> values = (List<Object>) value;
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = ((Number) values.get(i)).intValue();
        return result;
    }
    @SuppressWarnings("unchecked") private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) for (Object item : (List<Object>) value) result.add(String.valueOf(item));
        return result;
    }
    @SuppressWarnings("unchecked") private Map<String, Long> longMap(Object value) {
        Map<String, Long> result = new HashMap<>();
        if (value instanceof Map) for (Map.Entry<String, Object> item : ((Map<String, Object>) value).entrySet())
            result.put(item.getKey(), ((Number) item.getValue()).longValue());
        return result;
    }
    private String otherPlayer(DocumentSnapshot match, String uid) {
        return uid.equals(match.getString("player1Id")) ? match.getString("player2Id") : match.getString("player1Id");
    }
    private void requireParticipant(DocumentSnapshot match) {
        String uid = currentUserId();
        if (!match.exists() || uid == null || (!uid.equals(match.getString("player1Id"))
                && !uid.equals(match.getString("player2Id"))))
            throw new IllegalStateException("Niste ucesnik ove partije.");
    }
    private long value(Long value) { return value == null ? 0 : value; }
    private DocumentReference matchRef(String matchId) { return firebase.getFirestore().collection("matches").document(matchId); }
    private DocumentReference gameRef(String matchId) { return matchRef(matchId).collection("games").document("skocko"); }
    public void stopListening() { if (registration != null) { registration.remove(); registration = null; } }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Sinhronizacija Skočka nije uspela." : error.getLocalizedMessage(); }
}
