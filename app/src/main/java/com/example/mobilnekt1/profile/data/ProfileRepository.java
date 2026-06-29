package com.example.mobilnekt1.profile.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.domain.PlayerStats;
import com.example.mobilnekt1.profile.domain.UserProfile;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashMap;
import java.util.Map;

public final class ProfileRepository {
    private final FirebaseProvider firebase;
    private final UserRepository userRepository;
    private ListenerRegistration profileRegistration;
    private ListenerRegistration statsRegistration;
    private UserProfile profile;
    private PlayerStats stats = new PlayerStats();

    public ProfileRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
        userRepository = new UserRepository(context);
    }

    public void listen(ProfileListener listener) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { listener.onError("Sesija je istekla."); return; }
        userRepository.ensureCurrentUserDefaults(true, new GameActionCallback() {
            @Override public void onSuccess() { startListening(user, listener); }
            @Override public void onError(String message) { listener.onError(message); }
        });
    }

    private void startListening(FirebaseUser user, ProfileListener listener) {
        DocumentReference userRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference statsRef = firebase.getFirestore().collection("playerStats").document(user.getUid());
        profileRegistration = userRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) { listener.onError(message(error)); return; }
            if (snapshot == null || !snapshot.exists()) { listener.onError("Profil ne postoji."); return; }
            profile = UserRepository.toProfile(snapshot, user.getUid(), user.getEmail());
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
        userRepository.updateUser(values, callback);
    }

    public void stop() {
        if (profileRegistration != null) profileRegistration.remove();
        if (statsRegistration != null) statsRegistration.remove();
    }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Profil nije moguce ucitati." : error.getLocalizedMessage(); }
}
