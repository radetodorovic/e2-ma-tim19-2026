package com.example.mobilnekt1.tournament.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.tournament.domain.Tournament;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

public final class TournamentRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration registration;
    public TournamentRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void listen(TournamentListener listener) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null || user.isAnonymous()) { listener.onError("Turniri su dostupni registrovanim igracima."); return; }
        registration = firebase.getFirestore().collection("tournaments")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(message(error)); return; }
                    List<Tournament> result = new ArrayList<>();
                    if (snapshot != null) for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Tournament value = document.toObject(Tournament.class);
                        if (value != null && ("waiting".equals(value.status)
                                || value.participantIds.contains(user.getUid()))) {
                            value.id = document.getId(); result.add(value);
                        }
                    }
                    listener.onChanged(result, user.getUid());
                });
    }

    public void create(GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null || user.isAnonymous()) { callback.onError("Morate biti registrovani."); return; }
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference tournamentRef = firebase.getFirestore().collection("tournaments").document();
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot profile = transaction.get(userRef);
            requireAvailable(profile); Map<String, Object> tournament = new HashMap<>();
            tournament.put("creatorId", user.getUid()); tournament.put("status", "waiting");
            tournament.put("participantIds", Collections.singletonList(user.getUid()));
            Map<String, String> names = new HashMap<>(); names.put(user.getUid(), profile.getString("username"));
            tournament.put("participantNames", names); tournament.put("semifinalOneIds", new ArrayList<String>());
            tournament.put("semifinalTwoIds", new ArrayList<String>()); tournament.put("createdAt", FieldValue.serverTimestamp());
            tournament.put("semifinalOneMatchId", null); tournament.put("semifinalTwoMatchId", null);
            tournament.put("semifinalOneWinnerId", null); tournament.put("semifinalTwoWinnerId", null);
            tournament.put("finalMatchId", null); tournament.put("winnerId", null);
            tournament.put("updatedAt", FieldValue.serverTimestamp()); transaction.update(userRef, charged(profile));
            transaction.set(tournamentRef, tournament); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess()).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void join(String id, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null || user.isAnonymous()) { callback.onError("Morate biti registrovani."); return; }
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference tournamentRef = firebase.getFirestore().collection("tournaments").document(id);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot tournament = transaction.get(tournamentRef), profile = transaction.get(userRef);
            if (!tournament.exists() || !"waiting".equals(tournament.getString("status")))
                throw new IllegalStateException("Turnir vise ne prima igrace.");
            List<String> participants = strings(tournament.get("participantIds"));
            if (participants.contains(user.getUid())) return null;
            if (participants.size() >= 4) throw new IllegalStateException("Turnir je popunjen.");
            requireAvailable(profile); participants.add(user.getUid());
            Map<String, String> names = stringMap(tournament.get("participantNames"));
            names.put(user.getUid(), profile.getString("username"));
            Map<String, Object> values = new HashMap<>(); values.put("participantIds", participants);
            values.put("participantNames", names); values.put("updatedAt", FieldValue.serverTimestamp());
            if (participants.size() == 4) {
                Collections.shuffle(participants, new Random(id.hashCode()));
                String matchOneId = id + "_s1", matchTwoId = id + "_s2";
                values.put("participantIds", participants); values.put("status", "semifinals");
                values.put("semifinalOneIds", Arrays.asList(participants.get(0), participants.get(1)));
                values.put("semifinalTwoIds", Arrays.asList(participants.get(2), participants.get(3)));
                values.put("semifinalOneMatchId", matchOneId); values.put("semifinalTwoMatchId", matchTwoId);
                transaction.set(firebase.getFirestore().collection("matches").document(matchOneId),
                        tournamentMatch(id, "semifinal1", participants.get(0), participants.get(1), names));
                transaction.set(firebase.getFirestore().collection("matches").document(matchTwoId),
                        tournamentMatch(id, "semifinal2", participants.get(2), participants.get(3), names));
            }
            transaction.update(userRef, charged(profile)); transaction.update(tournamentRef, values); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess()).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void stop() { if (registration != null) registration.remove(); registration = null; }
    public void activateMatch(String matchId, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference matchRef = firebase.getFirestore().collection("matches").document(matchId);
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            if (!user.getUid().equals(match.getString("player1Id")) && !user.getUid().equals(match.getString("player2Id")))
                throw new IllegalStateException("Niste ucesnik ove partije.");
            Map<String,Object> values = new HashMap<>(); values.put("inGame", true);
            values.put("activeMatchId", matchId); values.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(userRef, values); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess()).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void advance(String tournamentId) {
        if (tournamentId == null || firebase.getCurrentUser() == null) return;
        DocumentReference tournamentRef = firebase.getFirestore().collection("tournaments").document(tournamentId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot tournament = transaction.get(tournamentRef);
            String status = tournament.getString("status");
            Map<String,String> names = stringMap(tournament.get("participantNames"));
            if ("semifinals".equals(status)) {
                String oneId = tournament.getString("semifinalOneMatchId");
                String twoId = tournament.getString("semifinalTwoMatchId");
                DocumentSnapshot one = transaction.get(firebase.getFirestore().collection("matches").document(oneId));
                DocumentSnapshot two = transaction.get(firebase.getFirestore().collection("matches").document(twoId));
                if (!terminal(one.getString("status")) || !terminal(two.getString("status"))) return null;
                String winnerOne = one.getString("winnerId"), winnerTwo = two.getString("winnerId");
                if (winnerOne == null || winnerTwo == null) throw new IllegalStateException("Nereseno polufinale mora da se ponovi.");
                String finalId = tournamentId + "_final"; Map<String,Object> values = new HashMap<>();
                values.put("semifinalOneWinnerId", winnerOne); values.put("semifinalTwoWinnerId", winnerTwo);
                values.put("finalMatchId", finalId); values.put("status", "final"); values.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(firebase.getFirestore().collection("matches").document(finalId),
                        tournamentMatch(tournamentId, "final", winnerOne, winnerTwo, names));
                transaction.update(tournamentRef, values);
            } else if ("final".equals(status)) {
                DocumentSnapshot finale = transaction.get(firebase.getFirestore().collection("matches")
                        .document(tournament.getString("finalMatchId")));
                if (terminal(finale.getString("status")) && finale.getString("winnerId") != null) {
                    transaction.update(tournamentRef, "winnerId", finale.getString("winnerId"),
                            "status", "finished", "updatedAt", FieldValue.serverTimestamp());
                }
            }
            return null;
        });
    }

    private Map<String, Object> tournamentMatch(String tournamentId, String stage, String player1,
                                                 String player2, Map<String, String> names) {
        Map<String,Object> value = new HashMap<>(); value.put("player1Id", player1); value.put("player1Name", names.get(player1));
        value.put("player2Id", player2); value.put("player2Name", names.get(player2)); value.put("status", "active");
        value.put("matchType", "tournament"); value.put("tournamentId", tournamentId); value.put("tournamentStage", stage);
        value.put("currentGame", "koZnaZna"); value.put("currentGameVersion", 1); value.put("currentTurnPlayerId", player1);
        value.put("winnerId", null); value.put("loserId", null); value.put("abandonedByUserId", null);
        value.put("player1Score", 0); value.put("player2Score", 0); value.put("player1StarDelta", 0); value.put("player2StarDelta", 0);
        value.put("player1TokenReward", 0); value.put("player2TokenReward", 0); value.put("player1InGame", true); value.put("player2InGame", true);
        value.put("settlementApplied", false); value.put("completedGames", new ArrayList<String>());
        value.put("createdAt", FieldValue.serverTimestamp()); value.put("startedAt", FieldValue.serverTimestamp());
        value.put("finishedAt", null); value.put("updatedAt", FieldValue.serverTimestamp()); return value;
    }
    private boolean terminal(String status) { return "finished".equals(status) || "abandoned".equals(status); }
    private void requireAvailable(DocumentSnapshot profile) {
        if (!profile.exists() || number(profile, "tokens") < 3) throw new IllegalStateException("Za turnir su potrebna 3 tokena.");
        if (Boolean.TRUE.equals(profile.getBoolean("inGame"))) throw new IllegalStateException("Vec ste u partiji.");
    }
    private Map<String, Object> charged(DocumentSnapshot profile) {
        Map<String, Object> values = new HashMap<>(); values.put("tokens", number(profile, "tokens") - 3);
        values.put("updatedAt", FieldValue.serverTimestamp()); return values;
    }
    @SuppressWarnings("unchecked") private List<String> strings(Object value) { return value instanceof List ? new ArrayList<>((List<String>) value) : new ArrayList<>(); }
    private Map<String, String> stringMap(Object value) { Map<String, String> result = new HashMap<>(); if (value instanceof Map)
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) if (entry.getKey() instanceof String && entry.getValue() instanceof String)
            result.put((String) entry.getKey(), (String) entry.getValue()); return result; }
    private long number(DocumentSnapshot value, String field) { Long number = value.getLong(field); return number == null ? 0 : number; }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Turnir nije dostupan." : error.getLocalizedMessage(); }
}
