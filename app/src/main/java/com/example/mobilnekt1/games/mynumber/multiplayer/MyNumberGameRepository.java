package com.example.mobilnekt1.games.mynumber.multiplayer;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.mynumber.MyNumberGenerator;
import com.example.mobilnekt1.games.mynumber.MyNumberRound;
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

public final class MyNumberGameRepository {
    private final FirebaseProvider firebase;
    private final MyNumberGenerator generator = new MyNumberGenerator();
    private ListenerRegistration registration;

    public MyNumberGameRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
    }

    public String currentUserId() {
        FirebaseUser user = firebase.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void initialize(String matchId, GameActionCallback callback) {
        MyNumberRound generated = generator.generate();
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            if (!transaction.get(gameRef).exists()) {
                transaction.set(gameRef, initialRound(match.getString("player1Id"),
                        match.getString("player2Id"), match.getString("player1Id"), 0, generated));
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void listen(String matchId, MyNumberGameListener listener) {
        stopListening();
        registration = gameRef(matchId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(message(error));
            } else if (snapshot != null && snapshot.exists()) {
                MyNumberGameState state = snapshot.toObject(MyNumberGameState.class);
                if (state != null) {
                    listener.onChanged(state);
                }
            }
        });
    }

    public void stopTarget(String matchId, GameActionCallback callback) {
        advanceRolling(matchId, "targetRolling", "numbersRolling", callback);
    }

    public void stopNumbers(String matchId, GameActionCallback callback) {
        advanceRolling(matchId, "numbersRolling", "playing", callback);
    }

    private void advanceRolling(String matchId, String expected, String next,
                                GameActionCallback callback) {
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            DocumentSnapshot game = transaction.get(gameRef);
            if (!expected.equals(game.getString("phase"))) {
                return null;
            }
            Long deadline = game.getLong("deadlineMillis");
            boolean starter = currentUserId().equals(game.getString("startingPlayerId"));
            if (!starter && deadline != null && deadline > System.currentTimeMillis()) {
                throw new IllegalStateException("STOP kontrolise igrac cija je runda.");
            }
            long duration = "playing".equals(next) ? 60_000L : 5_000L;
            transaction.update(gameRef,
                    "phase", next,
                    "deadlineMillis", System.currentTimeMillis() + duration,
                    "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void submitResult(String matchId, int result, GameActionCallback callback) {
        MyNumberRound nextRound = generator.generate();
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            DocumentSnapshot game = transaction.get(gameRef);
            if (!"playing".equals(game.getString("phase"))) {
                throw new IllegalStateException("Unos trenutno nije dozvoljen.");
            }
            boolean player1 = currentUserId().equals(match.getString("player1Id"));
            String submittedField = player1 ? "player1Submitted" : "player2Submitted";
            if (Boolean.TRUE.equals(game.getBoolean(submittedField))) {
                throw new IllegalStateException("Rezultat je vec predat.");
            }
            transaction.update(gameRef,
                    player1 ? "player1Result" : "player2Result", result,
                    submittedField, true,
                    "updatedAt", FieldValue.serverTimestamp());
            boolean otherSubmitted = Boolean.TRUE.equals(game.getBoolean(
                    player1 ? "player2Submitted" : "player1Submitted"));
            if (otherSubmitted) {
                scoreAndAdvance(transaction, match, game, gameRef,
                        player1 ? result : value(game.getLong("player1Result")),
                        player1 ? value(game.getLong("player2Result")) : result,
                        nextRound);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void finishExpired(String matchId, GameActionCallback callback) {
        MyNumberRound nextRound = generator.generate();
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            DocumentSnapshot game = transaction.get(gameRef);
            Long deadline = game.getLong("deadlineMillis");
            if (!"playing".equals(game.getString("phase")) || deadline == null
                    || deadline > System.currentTimeMillis()) {
                return null;
            }
            scoreAndAdvance(transaction, match, game, gameRef,
                    Boolean.TRUE.equals(game.getBoolean("player1Submitted"))
                            ? value(game.getLong("player1Result")) : null,
                    Boolean.TRUE.equals(game.getBoolean("player2Submitted"))
                            ? value(game.getLong("player2Result")) : null,
                    nextRound);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void scoreAndAdvance(com.google.firebase.firestore.Transaction transaction,
                                 DocumentSnapshot match, DocumentSnapshot game,
                                 DocumentReference gameRef, Integer p1, Integer p2,
                                 MyNumberRound nextRound) {
        int target = value(game.getLong("target"));
        int p1Points = 0;
        int p2Points = 0;
        boolean p1Exact = p1 != null && p1 == target;
        boolean p2Exact = p2 != null && p2 == target;
        if (p1Exact) p1Points = 10;
        if (p2Exact) p2Points = 10;
        if (!p1Exact && !p2Exact) {
            int d1 = p1 == null ? Integer.MAX_VALUE : Math.abs(target - p1);
            int d2 = p2 == null ? Integer.MAX_VALUE : Math.abs(target - p2);
            if (d1 < d2) p1Points = 5;
            else if (d2 < d1) p2Points = 5;
            else if (d1 != Integer.MAX_VALUE) {
                if (match.getString("player1Id").equals(game.getString("startingPlayerId"))) {
                    p1Points = 5;
                } else {
                    p2Points = 5;
                }
            }
        }
        if (p1Points != 0) transaction.update(gameRef, "player1Score", FieldValue.increment(p1Points));
        if (p2Points != 0) transaction.update(gameRef, "player2Score", FieldValue.increment(p2Points));
        int round = value(game.getLong("round"));
        if (round >= 1) {
            transaction.update(gameRef,
                    "phase", "finished", "deadlineMillis", 0,
                    "updatedAt", FieldValue.serverTimestamp());
        } else {
            Map<String, Object> reset = initialRound(match.getString("player1Id"),
                    match.getString("player2Id"), match.getString("player2Id"), 1, nextRound);
            reset.remove("player1Id");
            reset.remove("player2Id");
            reset.remove("player1Score");
            reset.remove("player2Score");
            transaction.update(gameRef, reset);
        }
    }

    private Map<String, Object> initialRound(String player1Id, String player2Id, String starter,
                                             int round, MyNumberRound generated) {
        Map<String, Object> data = new HashMap<>();
        data.put("round", round);
        data.put("phase", "targetRolling");
        data.put("player1Id", player1Id);
        data.put("player2Id", player2Id);
        data.put("startingPlayerId", starter);
        data.put("deadlineMillis", System.currentTimeMillis() + 5_000L);
        data.put("target", generated.getTarget());
        List<Integer> numbers = new ArrayList<>();
        for (int number : generated.getNumbers()) numbers.add(number);
        data.put("numbers", numbers);
        data.put("player1Result", null);
        data.put("player2Result", null);
        data.put("player1Submitted", false);
        data.put("player2Submitted", false);
        data.put("player1Score", 0);
        data.put("player2Score", 0);
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private int value(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private void requireParticipant(DocumentSnapshot match) {
        String uid = currentUserId();
        if (!match.exists() || uid == null || (!uid.equals(match.getString("player1Id"))
                && !uid.equals(match.getString("player2Id")))) {
            throw new IllegalStateException("Niste ucesnik ove partije.");
        }
    }

    private DocumentReference matchRef(String matchId) {
        return firebase.getFirestore().collection("matches").document(matchId);
    }

    private DocumentReference gameRef(String matchId) {
        return matchRef(matchId).collection("games").document("myNumber");
    }

    public void stopListening() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private String message(Exception error) {
        return error.getLocalizedMessage() == null ? "Sinhronizacija igre nije uspela."
                : error.getLocalizedMessage();
    }
}
