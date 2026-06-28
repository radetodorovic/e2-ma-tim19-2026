package com.example.mobilnekt1.profile.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.domain.UserProfile;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class UserRepository {
    public static final String DEFAULT_AVATAR_ID = "M1";
    public static final String DEFAULT_AVATAR_FRAME = "standard";

    private final FirebaseProvider firebase;

    public UserRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
    }

    public void getUserById(String userId, UserProfileCallback callback) {
        if (!isConfigured(callback)) {
            return;
        }
        firebase.getFirestore().collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("Korisnicki profil ne postoji.");
                        return;
                    }
                    callback.onSuccess(toProfile(snapshot, userId, null));
                })
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void updateUser(Map<String, Object> values, GameActionCallback callback) {
        FirebaseUser user = currentUser(callback);
        if (user == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>(values);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        userReference(user.getUid()).set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void updateUserOnlineStatus(boolean online, GameActionCallback callback) {
        updateSingleStatus("isOnline", online, callback);
    }

    public void updateUserInGameStatus(boolean inGame, GameActionCallback callback) {
        updateSingleStatus("inGame", inGame, callback);
    }

    public void addTokens(long amount, GameActionCallback callback) {
        updateCounters(amount, 0, callback);
    }

    public void addStars(long amount, GameActionCallback callback) {
        updateCounters(0, amount, callback);
    }

    public void updateStars(long totalStars, long weeklyStars, long monthlyStars,
                            GameActionCallback callback) {
        if (totalStars < 0 || weeklyStars < 0 || monthlyStars < 0) {
            callback.onError("Broj zvezda ne moze biti negativan.");
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("stars", totalStars);
        values.put("weeklyStars", weeklyStars);
        values.put("monthlyStars", monthlyStars);
        updateUser(values, callback);
    }

    public void ensureCurrentUserDefaults(boolean online, GameActionCallback callback) {
        FirebaseUser user = currentUser(callback);
        if (user == null) {
            return;
        }
        DocumentReference reference = userReference(user.getUid());
        reference.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                callback.onError("Korisnicki profil ne postoji.");
                return;
            }
            Map<String, Object> defaults = missingDefaults(snapshot, user, online);
            if (defaults.isEmpty()) {
                callback.onSuccess();
                return;
            }
            defaults.put("updatedAt", FieldValue.serverTimestamp());
            reference.set(defaults, SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(error -> callback.onError(message(error)));
        }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    static UserProfile toProfile(DocumentSnapshot snapshot, String userId, String authEmail) {
        UserProfile profile = snapshot.toObject(UserProfile.class);
        if (profile == null) {
            profile = new UserProfile();
        }
        profile.uid = userId;
        if (profile.email == null) {
            profile.email = authEmail == null ? "" : authEmail;
        }
        if (profile.username == null) {
            profile.username = "";
        }
        if (profile.region == null) {
            profile.region = "";
        }
        if (profile.avatarId == null) {
            profile.avatarId = DEFAULT_AVATAR_ID;
        }
        if (profile.avatarFrame == null) {
            profile.avatarFrame = DEFAULT_AVATAR_FRAME;
        }
        if (profile.qrCodeValue == null) {
            profile.qrCodeValue = "slagalica:user:" + userId;
        }
        return profile;
    }

    private Map<String, Object> missingDefaults(DocumentSnapshot snapshot, FirebaseUser user,
                                                boolean online) {
        Map<String, Object> values = new HashMap<>();
        putIfMissing(snapshot, values, "uid", user.getUid());
        putIfMissing(snapshot, values, "email", user.getEmail() == null ? "" : user.getEmail());
        putIfMissing(snapshot, values, "avatarId", DEFAULT_AVATAR_ID);
        putIfMissing(snapshot, values, "avatarFrame", DEFAULT_AVATAR_FRAME);
        putIfMissing(snapshot, values, "qrCodeValue", "slagalica:user:" + user.getUid());
        putIfMissing(snapshot, values, "tokens", 5L);
        putIfMissing(snapshot, values, "stars", 0L);
        putIfMissing(snapshot, values, "weeklyStars", 0L);
        putIfMissing(snapshot, values, "monthlyStars", 0L);
        putIfMissing(snapshot, values, "league", 0L);
        putIfMissing(snapshot, values, "inGame", false);
        putIfMissing(snapshot, values, "lastDailyTokenClaimAt", FieldValue.serverTimestamp());
        putIfMissing(snapshot, values, "createdAt", FieldValue.serverTimestamp());
        if (!Boolean.valueOf(online).equals(snapshot.getBoolean("isOnline"))) {
            values.put("isOnline", online);
        }
        if (!snapshot.contains("updatedAt")) {
            values.put("updatedAt", FieldValue.serverTimestamp());
        }
        return values;
    }

    private void updateSingleStatus(String field, boolean value, GameActionCallback callback) {
        Map<String, Object> values = new HashMap<>();
        values.put(field, value);
        updateUser(values, callback);
    }

    private void updateCounters(long tokenDelta, long starDelta, GameActionCallback callback) {
        FirebaseUser user = currentUser(callback);
        if (user == null) {
            return;
        }
        DocumentReference reference = userReference(user.getUid());
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(reference);
            long tokens = value(snapshot, "tokens");
            long stars = value(snapshot, "stars");
            long weeklyStars = value(snapshot, "weeklyStars");
            long monthlyStars = value(snapshot, "monthlyStars");
            long nextTokens = tokens + tokenDelta;
            long nextStars = Math.max(0, stars + starDelta);
            long nextWeeklyStars = Math.max(0, weeklyStars + starDelta);
            long nextMonthlyStars = Math.max(0, monthlyStars + starDelta);
            if (nextTokens < 0) {
                throw new IllegalArgumentException("Broj tokena ne moze biti negativan.");
            }
            Map<String, Object> updates = new HashMap<>();
            updates.put("tokens", nextTokens);
            updates.put("stars", nextStars);
            updates.put("weeklyStars", nextWeeklyStars);
            updates.put("monthlyStars", nextMonthlyStars);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(reference, updates);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private FirebaseUser currentUser(GameActionCallback callback) {
        if (!firebase.isConfigured()) {
            callback.onError("Firebase nije konfigurisan.");
            return null;
        }
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla.");
        }
        return user;
    }

    private boolean isConfigured(UserProfileCallback callback) {
        if (!firebase.isConfigured()) {
            callback.onError("Firebase nije konfigurisan.");
            return false;
        }
        return true;
    }

    private DocumentReference userReference(String userId) {
        return firebase.getFirestore().collection("users").document(userId);
    }

    private static void putIfMissing(DocumentSnapshot snapshot, Map<String, Object> values,
                                     String field, Object defaultValue) {
        if (!snapshot.contains(field)) {
            values.put(field, defaultValue);
        }
    }

    private static long value(DocumentSnapshot snapshot, String field) {
        Long value = snapshot.getLong(field);
        return value == null ? 0 : value;
    }

    private static String message(Exception error) {
        String message = error.getLocalizedMessage();
        return message == null ? "Korisnicki profil nije moguce azurirati." : message;
    }
}
