package com.example.mobilnekt1.profile.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.domain.PlayerStats;
import com.example.mobilnekt1.profile.domain.UserProfile;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public final class ProfileRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration profileRegistration;
    private ListenerRegistration statsRegistration;
    private UserProfile profile;
    private PlayerStats stats = new PlayerStats();

    public ProfileRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void listen(ProfileListener listener) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { listener.onError("Sesija je istekla."); return; }
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference statsRef = firebase.getFirestore().collection("playerStats").document(user.getUid());
        profileRegistration = userRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) { listener.onError(message(error)); return; }
            if (snapshot == null || !snapshot.exists()) { listener.onError("Profil ne postoji."); return; }
            profile = snapshot.toObject(UserProfile.class);
            if (profile == null) profile = new UserProfile();
            profile.uid = user.getUid();
            if (profile.email == null) profile.email = user.getEmail();
            if (profile.avatarId == null) profile.avatarId = "M1";
            if (profile.avatarFrame == null) profile.avatarFrame = "standard";
            if (profile.qrCodeValue == null) profile.qrCodeValue = "slagalica:user:" + user.getUid();
            listener.onChanged(profile, stats);
        });
        statsRegistration = statsRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) { listener.onError(message(error)); return; }
            if (snapshot != null && snapshot.exists()) {
                PlayerStats loaded = snapshot.toObject(PlayerStats.class);
                if (loaded != null) stats = loaded;
            }
            if (profile != null) listener.onChanged(profile, stats);
        });
    }

    public void updateAvatar(String avatarId, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        Map<String, Object> values = new HashMap<>();
        values.put("avatarId", avatarId);
        values.put("avatarFrame", profile == null ? "standard" : profile.avatarFrame);
        values.put("qrCodeValue", "slagalica:user:" + user.getUid());
        firebase.getFirestore().collection("users").document(user.getUid())
                .set(values, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void stop() {
        if (profileRegistration != null) profileRegistration.remove();
        if (statsRegistration != null) statsRegistration.remove();
    }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Profil nije moguce ucitati." : error.getLocalizedMessage(); }
}
