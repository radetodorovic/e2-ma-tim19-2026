package com.example.mobilnekt1.match.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.match.domain.Match;
import com.example.mobilnekt1.match.domain.MatchCodeGenerator;
import com.example.mobilnekt1.match.domain.MatchGameSequence;
import com.example.mobilnekt1.match.domain.MatchType;
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
    private final MatchCompletionRepository completionRepository;
    private ListenerRegistration registration;

    public MatchRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
        completionRepository = new MatchCompletionRepository(context);
    }

    public void createMatch(MatchCallback callback) {
        createMatch(MatchType.REGULAR, callback);
    }

    public void createFriendlyMatch(MatchCallback callback) {
        createMatch(MatchType.FRIENDLY, callback);
    }

    public void createMatch(String matchType, MatchCallback callback) {
        if (!MatchType.isSupported(matchType)) {
            callback.onError("Tip partije nije podrzan.");
            return;
        }
        withCurrentPlayer((user, username) ->
                createMatch(user, username, matchType, 0, callback), callback);
    }

    private void createMatch(FirebaseUser user, String username, String matchType, int attempt,
                             MatchCallback callback) {
        if (attempt >= MAX_CODE_ATTEMPTS) {
            callback.onError("Nije moguce generisati kod partije. Pokusajte ponovo.");
            return;
        }
        FirebaseFirestore firestore = firebase.getFirestore();
        String matchId = codeGenerator.generate();
        DocumentReference reference = firestore.collection("matches").document(matchId);
        DocumentReference userReference = firestore.collection("users").document(user.getUid());
        firestore.runTransaction(transaction -> {
            if (transaction.get(reference).exists()) {
                return false;
            }
            DocumentSnapshot userSnapshot = transaction.get(userReference);
            if (!userSnapshot.exists()) {
                throw new IllegalStateException("Korisnicki profil ne postoji.");
            }
            if (Boolean.TRUE.equals(userSnapshot.getBoolean("inGame"))) {
                throw new IllegalStateException("Vec ucestvujete u drugoj partiji.");
            }
            if (MatchType.REGULAR.equals(matchType)
                    && value(userSnapshot.getLong("tokens")) < 1) {
                throw new IllegalStateException("Nemate dovoljno tokena za regularnu partiju.");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("player1Id", user.getUid());
            data.put("player1Name", username);
            data.put("player2Id", null);
            data.put("player2Name", null);
            data.put("status", "waiting");
            data.put("matchType", matchType);
            data.put("currentGame", "none");
            data.put("currentGameVersion", 0);
            data.put("currentTurnPlayerId", user.getUid());
            data.put("winnerId", null);
            data.put("loserId", null);
            data.put("abandonedByUserId", null);
            data.put("player1Score", 0);
            data.put("player2Score", 0);
            data.put("player1StarDelta", 0);
            data.put("player2StarDelta", 0);
            data.put("player1TokenReward", 0);
            data.put("player2TokenReward", 0);
            data.put("player1InGame", false);
            data.put("player2InGame", false);
            data.put("settlementApplied", false);
            data.put("completedGames", new ArrayList<>());
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("startedAt", null);
            data.put("finishedAt", null);
            data.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(reference, data);
            return true;
        }).addOnSuccessListener(created -> {
            if (created) {
                callback.onSuccess(matchId);
            } else {
                createMatch(user, username, matchType, attempt + 1, callback);
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
                String matchType = snapshot.getString("matchType");
                if (!MatchType.isSupported(matchType)) matchType = MatchType.REGULAR;
                DocumentReference player1Ref = firebase.getFirestore()
                        .collection("users").document(player1Id);
                DocumentReference player2Ref = firebase.getFirestore()
                        .collection("users").document(user.getUid());
                DocumentSnapshot player1 = transaction.get(player1Ref);
                DocumentSnapshot player2 = transaction.get(player2Ref);
                if (!player1.exists() || !player2.exists()) {
                    throw new IllegalStateException("Korisnicki profil jednog igraca ne postoji.");
                }
                if (Boolean.TRUE.equals(player1.getBoolean("inGame"))
                        || Boolean.TRUE.equals(player2.getBoolean("inGame"))) {
                    throw new IllegalStateException("Jedan od igraca je vec u drugoj partiji.");
                }
                long tokenCost = MatchType.REGULAR.equals(matchType) ? 1 : 0;
                if (value(player1.getLong("tokens")) < tokenCost
                        || value(player2.getLong("tokens")) < tokenCost) {
                    throw new IllegalStateException("Oba igraca moraju imati token za regularnu partiju.");
                }
                transaction.update(player1Ref, activeUserValues(player1, tokenCost, matchId));
                transaction.update(player2Ref, activeUserValues(player2, tokenCost, matchId));
                Map<String, Object> matchValues = new HashMap<>();
                matchValues.put("player2Id", user.getUid());
                matchValues.put("player2Name", username);
                matchValues.put("status", "active");
                matchValues.put("currentGame", MatchGameSequence.firstGame());
                matchValues.put("currentGameVersion", FieldValue.increment(1));
                matchValues.put("player1InGame", true);
                matchValues.put("player2InGame", true);
                matchValues.put("startedAt", FieldValue.serverTimestamp());
                matchValues.put("updatedAt", FieldValue.serverTimestamp());
                transaction.update(reference, matchValues);
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

    public void finishMatch(String matchId, MatchCallback callback) {
        completionRepository.finishMatch(matchId, actionCallback(matchId, callback));
    }

    public void abandonMatch(String matchId, MatchCallback callback) {
        completionRepository.abandonMatch(matchId, actionCallback(matchId, callback));
    }

    private GameActionCallback actionCallback(String matchId, MatchCallback callback) {
        return new GameActionCallback() {
            @Override public void onSuccess() { callback.onSuccess(matchId); }
            @Override public void onError(String message) { callback.onError(message); }
        };
    }

    private Map<String, Object> activeUserValues(DocumentSnapshot user, long tokenCost,
                                                 String matchId) {
        Map<String, Object> values = new HashMap<>();
        values.put("tokens", value(user.getLong("tokens")) - tokenCost);
        values.put("inGame", true);
        values.put("activeMatchId", matchId);
        values.put("updatedAt", FieldValue.serverTimestamp());
        return values;
    }

    private long value(Long value) {
        return value == null ? 0 : value;
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
