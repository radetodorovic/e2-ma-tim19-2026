package com.example.mobilnekt1.games.stepbystep.multiplayer;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.content.GameContentRepository;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.games.stepbystep.StepByStepEngine;
import com.example.mobilnekt1.games.stepbystep.StepPuzzle;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public final class StepGameRepository {
    private final FirebaseProvider firebase;
    private final GameContentRepository contentRepository;
    private ListenerRegistration registration;

    public StepGameRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
        contentRepository = new GameContentRepository(firebase.getFirestore());
    }

    public String currentUserId() {
        FirebaseUser user = firebase.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void initialize(String matchId, GameActionCallback callback) {
        contentRepository.loadStepPuzzles(new GameContentRepository.Callback<List<StepPuzzle>>() {
            @Override public void onSuccess(List<StepPuzzle> puzzles) {
                initializeWithPuzzles(matchId, puzzles, callback);
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    private void initializeWithPuzzles(String matchId, List<StepPuzzle> puzzles,
                                       GameActionCallback callback) {
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            if (!transaction.get(gameRef).exists()) {
                Map<String, Object> data = new HashMap<>();
                data.put("round", 0);
                data.put("puzzleIndex", 0);
                data.put("solution", puzzles.get(0).solution);
                data.put("hints", Arrays.asList(puzzles.get(0).hints));
                data.put("round2Solution", puzzles.get(1).solution);
                data.put("round2Hints", Arrays.asList(puzzles.get(1).hints));
                data.put("phase", "main");
                data.put("player1Id", match.getString("player1Id"));
                data.put("player2Id", match.getString("player2Id"));
                data.put("activePlayerId", match.getString("player1Id"));
                data.put("deadlineMillis", System.currentTimeMillis()
                        + StepByStepEngine.ROUND_SECONDS * 1000L);
                data.put("player1Score", 0);
                data.put("player2Score", 0);
                data.put("player1SolvedStep", 0);
                data.put("player2SolvedStep", 0);
                data.put("eventVersion", 0);
                data.put("eventType", "none");
                data.put("eventPlayerId", null);
                data.put("eventPoints", 0);
                data.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(gameRef, data);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void listen(String matchId, StepGameListener listener) {
        stopListening();
        registration = gameRef(matchId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(message(error));
            } else if (snapshot != null && snapshot.exists()) {
                StepGameState state = snapshot.toObject(StepGameState.class);
                if (state != null) {
                    listener.onChanged(state);
                }
            }
        });
    }

    public void submitAnswer(String matchId, String answer, GameActionCallback callback) {
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot game = transaction.get(gameRef);
            requireGameParticipant(game);
            String phase = game.getString("phase");
            String activePlayerId = game.getString("activePlayerId");
            Long deadline = game.getLong("deadlineMillis");
            if ("finished".equals(phase) || !currentUserId().equals(activePlayerId)) {
                throw new IllegalStateException("Sacekajte svoj potez.");
            }
            if (deadline == null || deadline < System.currentTimeMillis()) {
                throw new IllegalStateException("Vreme za odgovor je isteklo.");
            }
            if (!StepByStepEngine.matches(answer, game.getString("solution"))) {
                throw new IllegalArgumentException("Netacan odgovor.");
            }
            int points;
            int solvedStep = 0;
            if ("steal".equals(phase)) {
                points = 5;
            } else {
                long remaining = Math.max(0, deadline - System.currentTimeMillis());
                int elapsed = StepByStepEngine.ROUND_SECONDS
                        - (int) Math.ceil(remaining / 1000.0);
                int hints = Math.min(7, 1 + Math.max(0, elapsed) / 10);
                points = StepByStepEngine.pointsForHint(hints);
                solvedStep = hints;
            }
            awardAndAdvance(transaction, game, gameRef, points, solvedStep);
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
            String phase = game.getString("phase");
            if (deadline == null || deadline > System.currentTimeMillis() || "finished".equals(phase)) {
                return null;
            }
            if ("main".equals(phase)) {
                String starter = game.getString("activePlayerId");
                String opponent = starter.equals(game.getString("player1Id"))
                        ? game.getString("player2Id") : game.getString("player1Id");
                transaction.update(gameRef,
                        "phase", "steal",
                        "activePlayerId", opponent,
                        "deadlineMillis", System.currentTimeMillis()
                                + StepByStepEngine.STEAL_SECONDS * 1000L,
                        "updatedAt", FieldValue.serverTimestamp());
            } else {
                advanceRound(transaction, game, gameRef);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void awardAndAdvance(com.google.firebase.firestore.Transaction transaction,
                                 DocumentSnapshot game, DocumentReference gameRef, int points,
                                 int solvedStep) {
        String player1Id = game.getString("player1Id");
        String scoreField = currentUserId().equals(player1Id) ? "player1Score" : "player2Score";
        transaction.update(gameRef,
                scoreField, FieldValue.increment(points),
                "eventVersion", FieldValue.increment(1),
                "eventType", "correctAnswer",
                "eventPlayerId", currentUserId(),
                "eventPoints", points);
        if (solvedStep > 0) {
            transaction.update(gameRef,
                    currentUserId().equals(player1Id) ? "player1SolvedStep" : "player2SolvedStep",
                    solvedStep);
        }
        advanceRound(transaction, game, gameRef);
    }

    private void advanceRound(com.google.firebase.firestore.Transaction transaction,
                              DocumentSnapshot game, DocumentReference gameRef) {
        Long roundValue = game.getLong("round");
        int round = roundValue == null ? 0 : roundValue.intValue();
        if (round >= 1) {
            transaction.update(gameRef,
                    "phase", "finished",
                    "activePlayerId", null,
                    "deadlineMillis", 0,
                    "updatedAt", FieldValue.serverTimestamp());
            return;
        }
        transaction.update(gameRef,
                "round", 1,
                "puzzleIndex", 1,
                "solution", game.getString("round2Solution"),
                "hints", game.get("round2Hints"),
                "phase", "main",
                "activePlayerId", game.getString("player2Id"),
                "deadlineMillis", System.currentTimeMillis()
                        + StepByStepEngine.ROUND_SECONDS * 1000L,
                "updatedAt", FieldValue.serverTimestamp());
    }

    private void requireParticipant(DocumentSnapshot match) {
        String uid = currentUserId();
        if (!match.exists() || uid == null || (!uid.equals(match.getString("player1Id"))
                && !uid.equals(match.getString("player2Id")))) {
            throw new IllegalStateException("Niste ucesnik ove partije.");
        }
    }

    private void requireGameParticipant(DocumentSnapshot game) {
        String uid = currentUserId();
        if (!game.exists() || uid == null || (!uid.equals(game.getString("player1Id"))
                && !uid.equals(game.getString("player2Id")))) {
            throw new IllegalStateException("Niste ucesnik ove partije.");
        }
    }

    private DocumentReference matchRef(String matchId) {
        return firebase.getFirestore().collection("matches").document(matchId);
    }

    private DocumentReference gameRef(String matchId) {
        return matchRef(matchId).collection("games").document("stepByStep");
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
