package com.example.mobilnekt1.match.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.match.domain.Match;
import com.example.mobilnekt1.match.domain.MatchCodeGenerator;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

public final class MatchRepository {
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final FirebaseProvider firebase;
    private final MatchCodeGenerator codeGenerator = new MatchCodeGenerator();
    private ListenerRegistration registration;

    public MatchRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
    }

    public void createMatch(MatchCallback callback) {
        withCurrentPlayer((user, username) -> createMatch(user, username, 0, callback), callback);
    }

    private void createMatch(FirebaseUser user, String username, int attempt,
                             MatchCallback callback) {
        if (attempt >= MAX_CODE_ATTEMPTS) {
            callback.onError("Nije moguce generisati kod partije. Pokusajte ponovo.");
            return;
        }
        FirebaseFirestore firestore = firebase.getFirestore();
        String matchId = codeGenerator.generate();
        DocumentReference reference = firestore.collection("matches").document(matchId);
        firestore.runTransaction(transaction -> {
            if (transaction.get(reference).exists()) {
                return false;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("player1Id", user.getUid());
            data.put("player1Name", username);
            data.put("player2Id", null);
            data.put("player2Name", null);
            data.put("status", "waiting");
            data.put("currentGame", "none");
            data.put("currentGameVersion", 0);
            data.put("currentTurnPlayerId", user.getUid());
            data.put("winnerId", null);
            data.put("player1Score", 0);
            data.put("player2Score", 0);
            data.put("completedGames", new ArrayList<>());
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(reference, data);
            return true;
        }).addOnSuccessListener(created -> {
            if (created) {
                callback.onSuccess(matchId);
            } else {
                createMatch(user, username, attempt + 1, callback);
            }
        }).addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    public void joinMatch(String rawMatchId, MatchCallback callback) {
        String matchId = MatchCodeGenerator.normalize(rawMatchId);
        if (!MatchCodeGenerator.isValid(matchId)) {
            callback.onError("Kod partije mora imati 6 ispravnih znakova.");
            return;
        }
        withCurrentPlayer((user, username) -> {
            DocumentReference reference = firebase.getFirestore()
                    .collection("matches").document(matchId);
            firebase.getFirestore().runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(reference);
                if (!snapshot.exists()) {
                    throw new IllegalStateException("Partija sa tim kodom ne postoji.");
                }
                String player1Id = snapshot.getString("player1Id");
                String player2Id = snapshot.getString("player2Id");
                String status = snapshot.getString("status");
                if (user.getUid().equals(player1Id) || user.getUid().equals(player2Id)) {
                    return null;
                }
                if (!"waiting".equals(status) || player2Id != null) {
                    throw new IllegalStateException("Partija je vec popunjena ili zavrsena.");
                }
                transaction.update(reference,
                        "player2Id", user.getUid(),
                        "player2Name", username,
                        "status", "active",
                        "updatedAt", FieldValue.serverTimestamp());
                return null;
            }).addOnSuccessListener(unused -> callback.onSuccess(matchId))
                    .addOnFailureListener(error -> callback.onError(messageFor(error)));
        }, callback);
    }

    public void listen(String matchId, MatchListener listener) {
        stopListening();
        if (!ensureReady(listener)) {
            return;
        }
        registration = firebase.getFirestore().collection("matches").document(matchId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(messageFor(error));
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        listener.onError("Partija vise ne postoji.");
                        return;
                    }
                    Match match = snapshot.toObject(Match.class);
                    if (match != null) {
                        match.id = snapshot.getId();
                        listener.onChanged(match);
                    }
                });
    }

    public void selectGame(String matchId, String game, MatchCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (!ensureReady(callback) || user == null) {
            if (user == null) {
                callback.onError("Sesija je istekla. Prijavite se ponovo.");
            }
            return;
        }
        DocumentReference reference = firebase.getFirestore().collection("matches").document(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(reference);
            if (!snapshot.exists() || !"active".equals(snapshot.getString("status"))) {
                throw new IllegalStateException("Sacekajte da se drugi igrac pridruzi.");
            }
            Object completedValue = snapshot.get("completedGames");
            if (completedValue instanceof java.util.List
                    && ((java.util.List<?>) completedValue).contains(game)) {
                throw new IllegalStateException("Ova igra je vec odigrana u trenutnoj partiji.");
            }
            String player1Id = snapshot.getString("player1Id");
            String player2Id = snapshot.getString("player2Id");
            if (!user.getUid().equals(player1Id) && !user.getUid().equals(player2Id)) {
                throw new IllegalStateException("Niste ucesnik ove partije.");
            }
            transaction.update(reference,
                    "currentGame", game,
                    "currentGameVersion", FieldValue.increment(1),
                    "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(matchId))
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    public void stopListening() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void withCurrentPlayer(PlayerOperation operation, MatchCallback callback) {
        if (!ensureReady(callback)) {
            return;
        }
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla. Prijavite se ponovo.");
            return;
        }
        firebase.getFirestore().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.getString("username");
                    if (username == null || username.trim().isEmpty()) {
                        username = user.getDisplayName();
                    }
                    if (username == null || username.trim().isEmpty()) {
                        username = user.getEmail() == null ? "Igrac" : user.getEmail();
                    }
                    operation.run(user, username);
                })
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    private boolean ensureReady(MatchCallback callback) {
        if (!firebase.isConfigured()) {
            callback.onError("Firebase nije konfigurisan. Dodajte app/google-services.json.");
            return false;
        }
        return true;
    }

    private boolean ensureReady(MatchListener listener) {
        if (!firebase.isConfigured()) {
            listener.onError("Firebase nije konfigurisan. Dodajte app/google-services.json.");
            return false;
        }
        return true;
    }

    private String messageFor(Exception error) {
        String message = error.getLocalizedMessage();
        return message == null ? "Pristup partiji nije uspeo." : message;
    }

    private interface PlayerOperation {
        void run(FirebaseUser user, String username);
    }
}
