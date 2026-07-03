package com.example.mobilnekt1.challenges.data;

import android.content.Context;
import com.example.mobilnekt1.challenges.domain.Challenge;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

public final class ChallengeRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration registration;

    public ChallengeRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void listen(ChallengeListener listener) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            listener.onError("Izazovi su dostupni registrovanim igracima."); return;
        }
        firebase.getFirestore().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(profile -> {
                    String region = profile.getString("region");
                    if (region == null || region.isEmpty()) { listener.onError("Profil nema region."); return; }
                    registration = firebase.getFirestore().collection("challenges")
                            .whereEqualTo("region", region)
                            .addSnapshotListener((snapshot, error) -> {
                                if (error != null) { listener.onError(message(error)); return; }
                                List<Challenge> result = new ArrayList<>();
                                if (snapshot != null) for (DocumentSnapshot document : snapshot.getDocuments()) {
                                    Challenge value = document.toObject(Challenge.class);
                                    if (value != null && ("open".equals(value.status) || "active".equals(value.status)
                                            || ("finished".equals(value.status) && value.participantIds.contains(user.getUid())))) {
                                        value.id = document.getId(); result.add(value);
                                    }
                                }
                                listener.onChanged(result, user.getUid());
                            });
                }).addOnFailureListener(error -> listener.onError(message(error)));
    }

    public void create(long stars, long tokens, GameActionCallback callback) {
        if (stars < 0 || stars > 10 || tokens < 0 || tokens > 2) {
            callback.onError("Ulog je najvise 10 zvezda i 2 tokena."); return;
        }
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null || user.isAnonymous()) { callback.onError("Morate biti registrovani."); return; }
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference challengeRef = firebase.getFirestore().collection("challenges").document();
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot profile = transaction.get(userRef);
            requireFunds(profile, stars, tokens);
            Map<String, Object> challenge = new HashMap<>();
            challenge.put("creatorId", user.getUid()); challenge.put("creatorName", profile.getString("username"));
            challenge.put("region", profile.getString("region")); challenge.put("status", "open");
            challenge.put("starStake", stars); challenge.put("tokenStake", tokens);
            challenge.put("participantIds", Collections.singletonList(user.getUid()));
            Map<String, String> names = new HashMap<>();
            names.put(user.getUid(), profile.getString("username"));
            challenge.put("participantNames", names);
            challenge.put("participantScores", new HashMap<String, Long>());
            challenge.put("claimedIds", new ArrayList<String>());
            challenge.put("createdAt", FieldValue.serverTimestamp()); challenge.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(userRef, wager(profile, stars, tokens)); transaction.set(challengeRef, challenge); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void join(String id, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null || user.isAnonymous()) { callback.onError("Morate biti registrovani."); return; }
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference challengeRef = firebase.getFirestore().collection("challenges").document(id);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot challenge = transaction.get(challengeRef);
            DocumentSnapshot profile = transaction.get(userRef);
            if (!challenge.exists() || !"open".equals(challenge.getString("status")))
                throw new IllegalStateException("Izazov vise nije otvoren.");
            List<String> participants = strings(challenge.get("participantIds"));
            if (participants.contains(user.getUid())) throw new IllegalStateException("Vec ucestvujete u izazovu.");
            if (participants.size() >= 4) throw new IllegalStateException("Izazov je popunjen.");
            if (!Objects.equals(profile.getString("region"), challenge.getString("region")))
                throw new IllegalStateException("Izazov pripada drugom regionu.");
            long stars = number(challenge, "starStake"), tokens = number(challenge, "tokenStake");
            requireFunds(profile, stars, tokens); participants.add(user.getUid());
            Map<String, Object> updates = new HashMap<>(); updates.put("participantIds", participants);
            Map<String, String> names = stringMap(challenge.get("participantNames"));
            names.put(user.getUid(), profile.getString("username")); updates.put("participantNames", names);
            updates.put("status", participants.size() == 4 ? "active" : "open");
            updates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(userRef, wager(profile, stars, tokens)); transaction.update(challengeRef, updates); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void start(String id, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference reference = firebase.getFirestore().collection("challenges").document(id);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot challenge = transaction.get(reference);
            if (!challenge.exists() || !"open".equals(challenge.getString("status")))
                throw new IllegalStateException("Izazov vise nije otvoren.");
            if (!user.getUid().equals(challenge.getString("creatorId")))
                throw new IllegalStateException("Samo kreator moze pokrenuti izazov.");
            if (strings(challenge.get("participantIds")).size() < 2)
                throw new IllegalStateException("Potreban je bar jos jedan igrac.");
            Map<String, Object> values = new HashMap<>(); values.put("status", "active");
            values.put("updatedAt", FieldValue.serverTimestamp()); transaction.update(reference, values); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void cancel(String id, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference challengeRef = firebase.getFirestore().collection("challenges").document(id);
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot challenge = transaction.get(challengeRef), profile = transaction.get(userRef);
            List<String> participants = strings(challenge.get("participantIds"));
            if (!"open".equals(challenge.getString("status")) || !user.getUid().equals(challenge.getString("creatorId")))
                throw new IllegalStateException("Izazov nije moguce otkazati.");
            if (participants.size() != 1) throw new IllegalStateException("Izazov sa prihvacenim ucesnicima se ne moze otkazati.");
            Map<String, Object> refund = new HashMap<>();
            refund.put("stars", number(profile, "stars") + number(challenge, "starStake"));
            refund.put("tokens", number(profile, "tokens") + number(challenge, "tokenStake"));
            refund.put("updatedAt", FieldValue.serverTimestamp()); transaction.update(userRef, refund);
            transaction.update(challengeRef, "status", "cancelled", "updatedAt", FieldValue.serverTimestamp()); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void stop() { if (registration != null) registration.remove(); registration = null; }

    public void saveGameScore(String challengeId, String game, int score,
                              GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference challengeRef = firebase.getFirestore().collection("challenges").document(challengeId);
        DocumentReference runRef = challengeRef.collection("runs").document(user.getUid());
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot challenge = transaction.get(challengeRef);
            DocumentSnapshot run = transaction.get(runRef);
            if (!challenge.exists() || !"active".equals(challenge.getString("status"))
                    || !strings(challenge.get("participantIds")).contains(user.getUid()))
                throw new IllegalStateException("Niste u aktivnom izazovu.");
            List<String> completed = run.exists() ? strings(run.get("completedGames")) : new ArrayList<>();
            if (completed.contains(game)) return null;
            completed.add(game);
            Map<String, Object> values = new HashMap<>(); values.put("userId", user.getUid());
            values.put("completedGames", completed); values.put("scores." + game, score);
            values.put("totalScore", (run.exists() ? number(run, "totalScore") : 0) + score);
            values.put("finished", completed.size() == 6); values.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(runRef, values, SetOptions.merge()); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void publishFinishedScore(String challengeId, long totalScore,
                                     GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference reference = firebase.getFirestore().collection("challenges").document(challengeId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot challenge = transaction.get(reference);
            if (!challenge.exists() || !strings(challenge.get("participantIds")).contains(user.getUid()))
                throw new IllegalStateException("Niste u izazovu.");
            Map<String, Long> scores = longMap(challenge.get("participantScores"));
            if (scores.containsKey(user.getUid())) return null;
            scores.put(user.getUid(), totalScore);
            Map<String, Object> values = new HashMap<>(); values.put("participantScores", scores);
            if (scores.size() == strings(challenge.get("participantIds")).size()) values.put("status", "finished");
            values.put("updatedAt", FieldValue.serverTimestamp()); transaction.update(reference, values); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void claimReward(String challengeId, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        DocumentReference challengeRef = firebase.getFirestore().collection("challenges").document(challengeId);
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot challenge = transaction.get(challengeRef), profile = transaction.get(userRef);
            if (!"finished".equals(challenge.getString("status"))) throw new IllegalStateException("Izazov jos nije zavrsen.");
            List<String> participants = strings(challenge.get("participantIds"));
            List<String> claimed = strings(challenge.get("claimedIds"));
            if (!participants.contains(user.getUid())) throw new IllegalStateException("Niste ucesnik.");
            if (claimed.contains(user.getUid())) return null;
            List<Map.Entry<String, Long>> ranking = new ArrayList<>(longMap(challenge.get("participantScores")).entrySet());
            Collections.sort(ranking, (a, b) -> Long.compare(b.getValue(), a.getValue()));
            int place = -1; for (int i = 0; i < ranking.size(); i++) if (ranking.get(i).getKey().equals(user.getUid())) place = i;
            long starStake = number(challenge, "starStake"), tokenStake = number(challenge, "tokenStake");
            long starPool = starStake * participants.size(), tokenPool = tokenStake * participants.size();
            long winnerStars = starPool * 75 / 100, winnerTokens = tokenPool * 75 / 100;
            long starReward = place == 0 ? winnerStars
                    : place == 1 ? Math.min(starStake, starPool - winnerStars) : 0;
            long tokenReward = place == 0 ? winnerTokens
                    : place == 1 ? Math.min(tokenStake, tokenPool - winnerTokens) : 0;
            Map<String, Object> userValues = new HashMap<>(); userValues.put("stars", number(profile, "stars") + starReward);
            userValues.put("tokens", number(profile, "tokens") + tokenReward); userValues.put("updatedAt", FieldValue.serverTimestamp());
            claimed.add(user.getUid()); transaction.update(userRef, userValues);
            transaction.update(challengeRef, "claimedIds", claimed, "updatedAt", FieldValue.serverTimestamp()); return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }
    private void requireFunds(DocumentSnapshot user, long stars, long tokens) {
        if (!user.exists() || number(user, "stars") < stars || number(user, "tokens") < tokens)
            throw new IllegalStateException("Nemate dovoljno zvezda ili tokena.");
    }
    private Map<String, Object> wager(DocumentSnapshot user, long stars, long tokens) {
        Map<String, Object> values = new HashMap<>(); values.put("stars", number(user, "stars") - stars);
        values.put("tokens", number(user, "tokens") - tokens); values.put("updatedAt", FieldValue.serverTimestamp()); return values;
    }
    @SuppressWarnings("unchecked") private List<String> strings(Object value) {
        return value instanceof List ? new ArrayList<>((List<String>) value) : new ArrayList<>();
    }
    private Map<String, Long> longMap(Object value) {
        Map<String, Long> result = new HashMap<>();
        if (value instanceof Map) for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet())
            if (entry.getKey() instanceof String && entry.getValue() instanceof Number)
                result.put((String) entry.getKey(), ((Number) entry.getValue()).longValue());
        return result;
    }
    private Map<String, String> stringMap(Object value) {
        Map<String, String> result = new HashMap<>();
        if (value instanceof Map) for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet())
            if (entry.getKey() instanceof String && entry.getValue() instanceof String)
                result.put((String) entry.getKey(), (String) entry.getValue());
        return result;
    }
    private long number(DocumentSnapshot value, String field) { Long result = value.getLong(field); return result == null ? 0 : result; }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Izazov nije dostupan." : error.getLocalizedMessage(); }
}
