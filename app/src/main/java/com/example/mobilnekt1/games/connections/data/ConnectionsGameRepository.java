package com.example.mobilnekt1.games.connections.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.content.GameContentRepository;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConnectionsGameRepository {
    private static final long PHASE_MILLIS = 30_000L;
    private final FirebaseProvider firebase;
    private final GameContentRepository contentRepository;
    private ListenerRegistration registration;

    public ConnectionsGameRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
        contentRepository = new GameContentRepository(firebase.getFirestore());
    }
    public String currentUserId() { FirebaseUser user = firebase.getCurrentUser(); return user == null ? null : user.getUid(); }

    public void initialize(String matchId, GameActionCallback callback) {
        contentRepository.loadConnectionsPuzzles(
                new GameContentRepository.Callback<List<GameContentRepository.ConnectionsPuzzle>>() {
                    @Override public void onSuccess(List<GameContentRepository.ConnectionsPuzzle> puzzles) {
                        initializeWithPuzzles(matchId, puzzles, callback);
                    }
                    @Override public void onError(String message) { callback.onError(message); }
                });
    }

    private void initializeWithPuzzles(String matchId,
                                       List<GameContentRepository.ConnectionsPuzzle> puzzles,
                                       GameActionCallback callback) {
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireMatchParticipant(match);
            if (!transaction.get(gameRef).exists()) {
                Map<String, Object> data = roundData(0, match.getString("player1Id"),
                        match.getString("abandonedByUserId"), puzzles.get(0));
                data.put("round2LeftItems", puzzles.get(1).leftItems);
                data.put("round2RightItems", puzzles.get(1).rightItems);
                data.put("round2CorrectMatches", puzzles.get(1).correctMatches);
                data.put("player1Id", match.getString("player1Id"));
                data.put("player2Id", match.getString("player2Id"));
                data.put("player1Score", 0);
                data.put("player2Score", 0);
                data.put("player1CorrectPairs", 0);
                data.put("player1AttemptedPairs", 0);
                data.put("player2CorrectPairs", 0);
                data.put("player2AttemptedPairs", 0);
                transaction.set(gameRef, data);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void listen(String matchId, ConnectionsGameListener listener) {
        stopListening();
        registration = gameRef(matchId).addSnapshotListener((snapshot, error) -> {
            if (error != null) listener.onError(message(error));
            else if (snapshot != null && snapshot.exists()) {
                com.example.mobilnekt1.games.connections.domain.ConnectionsGameState state =
                        snapshot.toObject(com.example.mobilnekt1.games.connections.domain.ConnectionsGameState.class);
                if (state != null) listener.onChanged(state);
            }
        });
    }

    public void choose(String matchId, int rightIndex, GameActionCallback callback) {
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot game = transaction.get(gameRef);
            requireGameParticipant(game);
            if ("finished".equals(game.getString("phase"))
                    || !currentUserId().equals(game.getString("activePlayerId"))) {
                throw new IllegalStateException("Sacekajte svoj potez.");
            }
            Long deadline = game.getLong("deadlineMillis");
            if (deadline == null || deadline < System.currentTimeMillis()) {
                throw new IllegalStateException("Vreme je isteklo.");
            }
            int currentLeft = intValue(game.getLong("currentLeft"));
            List<Long> matches = longList(game.get("correctMatches"));
            List<Long> solved = longList(game.get("solvedLeft"));
            if (currentLeft < 0 || currentLeft >= matches.size()) return null;
            boolean correct = matches.get(currentLeft).intValue() == rightIndex;
            boolean player1 = currentUserId().equals(game.getString("player1Id"));
            Map<String, Object> updates = new HashMap<>();
            updates.put(player1 ? "player1AttemptedPairs" : "player2AttemptedPairs", FieldValue.increment(1));
            if (correct) {
                if (!solved.contains((long) currentLeft)) solved.add((long) currentLeft);
                updates.put("solvedLeft", solved);
                updates.put(player1 ? "player1Score" : "player2Score", FieldValue.increment(2));
                updates.put(player1 ? "player1CorrectPairs" : "player2CorrectPairs", FieldValue.increment(1));
            }
            advanceAfterAttempt(game, updates, solved, currentLeft);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(gameRef, updates);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void advanceExpired(String matchId, GameActionCallback callback) {
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot game = transaction.get(gameRef);
            requireGameParticipant(game);
            Long deadline = game.getLong("deadlineMillis");
            if (deadline == null || deadline > System.currentTimeMillis()
                    || "finished".equals(game.getString("phase"))) return null;
            Map<String, Object> updates = new HashMap<>();
            if ("main".equals(game.getString("phase"))) startStealOrNextRound(game, updates,
                    longList(game.get("solvedLeft")));
            else startNextRoundOrFinish(game, updates);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(gameRef, updates);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void advanceAfterAttempt(DocumentSnapshot game, Map<String, Object> updates,
                                     List<Long> solved, int currentLeft) {
        if ("main".equals(game.getString("phase"))) {
            int next = currentLeft + 1;
            if (next < 5) updates.put("currentLeft", next);
            else startStealOrNextRound(game, updates, solved);
        } else {
            int next = nextUnsolved(solved, currentLeft + 1);
            if (next >= 0) updates.put("currentLeft", next);
            else startNextRoundOrFinish(game, updates);
        }
    }

    private void startStealOrNextRound(DocumentSnapshot game, Map<String, Object> updates,
                                       List<Long> solved) {
        int next = nextUnsolved(solved, 0);
        if (next < 0) { startNextRoundOrFinish(game, updates); return; }
        String starter = game.getString("startingPlayerId");
        String opponent = starter.equals(game.getString("player1Id"))
                ? game.getString("player2Id") : game.getString("player1Id");
        if (opponent.equals(game.getString("abandonedPlayerId"))) {
            startNextRoundOrFinish(game, updates);
            return;
        }
        updates.put("phase", "steal");
        updates.put("activePlayerId", opponent);
        updates.put("currentLeft", next);
        updates.put("deadlineMillis", System.currentTimeMillis() + PHASE_MILLIS);
    }

    private void startNextRoundOrFinish(DocumentSnapshot game, Map<String, Object> updates) {
        int round = intValue(game.getLong("round"));
        if (round >= 1) {
            updates.put("phase", "finished");
            updates.put("activePlayerId", null);
            updates.put("deadlineMillis", 0);
            return;
        }
        String starter = game.getString("player2Id");
        if (starter.equals(game.getString("abandonedPlayerId"))) {
            updates.put("phase", "finished");
            updates.put("activePlayerId", null);
            updates.put("deadlineMillis", 0);
            return;
        }
        updates.putAll(roundData(1, starter, game.getString("abandonedPlayerId"), new GameContentRepository.ConnectionsPuzzle(
                stringList(game.get("round2LeftItems")), stringList(game.get("round2RightItems")),
                integerList(game.get("round2CorrectMatches")))));
    }

    private Map<String, Object> roundData(int round, String starter, String abandonedPlayerId,
                                          GameContentRepository.ConnectionsPuzzle puzzle) {
        Map<String, Object> data = new HashMap<>();
        data.put("round", round);
        data.put("phase", "main");
        data.put("startingPlayerId", starter);
        data.put("activePlayerId", starter);
        data.put("abandonedPlayerId", abandonedPlayerId);
        data.put("deadlineMillis", starter.equals(abandonedPlayerId)
                ? 0 : System.currentTimeMillis() + PHASE_MILLIS);
        data.put("currentLeft", 0);
        data.put("solvedLeft", new ArrayList<>());
        data.put("leftItems", puzzle.leftItems);
        data.put("rightItems", puzzle.rightItems);
        data.put("correctMatches", puzzle.correctMatches);
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private int nextUnsolved(List<Long> solved, int start) {
        for (int i = start; i < 5; i++) if (!solved.contains((long) i)) return i;
        return -1;
    }

    @SuppressWarnings("unchecked") private List<Long> longList(Object value) {
        List<Long> result = new ArrayList<>();
        if (value instanceof List) for (Object item : (List<?>) value) result.add(((Number) item).longValue());
        return result;
    }
    @SuppressWarnings("unchecked") private List<String> stringList(Object value) {
        return value instanceof List ? new ArrayList<>((List<String>) value) : new ArrayList<>();
    }
    private List<Integer> integerList(Object value) {
        List<Integer> result = new ArrayList<>();
        if (value instanceof List) for (Object item : (List<?>) value) result.add(((Number) item).intValue());
        return result;
    }
    private int intValue(Long value) { return value == null ? 0 : value.intValue(); }
    private void requireMatchParticipant(DocumentSnapshot match) {
        String uid = currentUserId();
        if (!match.exists() || uid == null || (!uid.equals(match.getString("player1Id")) && !uid.equals(match.getString("player2Id"))))
            throw new IllegalStateException("Niste ucesnik ove partije.");
    }
    private void requireGameParticipant(DocumentSnapshot game) {
        String uid = currentUserId();
        if (!game.exists() || uid == null || (!uid.equals(game.getString("player1Id")) && !uid.equals(game.getString("player2Id"))))
            throw new IllegalStateException("Niste ucesnik ove partije.");
    }
    private DocumentReference matchRef(String id) { return firebase.getFirestore().collection("matches").document(id); }
    private DocumentReference gameRef(String id) { return matchRef(id).collection("games").document("spojnice"); }
    public void stopListening() { if (registration != null) { registration.remove(); registration = null; } }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Sinhronizacija Spojnica nije uspela." : error.getLocalizedMessage(); }
}
