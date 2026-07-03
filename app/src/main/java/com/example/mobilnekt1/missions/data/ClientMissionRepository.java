package com.example.mobilnekt1.missions.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.profile.domain.LeaguePolicy;
import com.example.mobilnekt1.match.domain.StarTokenProgress;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class ClientMissionRepository {
    public static final String WIN_MATCH = "winMatch";
    public static final String SEND_CHAT = "sendChat";
    public static final String PLAY_FRIENDLY = "playFriendly";
    public static final String WIN_TOURNAMENT = "winTournament";
    private static final List<String> KEYS = Arrays.asList(WIN_MATCH, SEND_CHAT, PLAY_FRIENDLY, WIN_TOURNAMENT);
    private final FirebaseProvider firebase;

    public ClientMissionRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void complete(String key) {
        if (!KEYS.contains(key) || firebase.getCurrentUser() == null) return;
        String uid = firebase.getCurrentUser().getUid();
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        DocumentReference userRef = firebase.getFirestore().collection("users").document(uid);
        DocumentReference missionRef = userRef.collection("dailyMissions").document(day);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot mission = transaction.get(missionRef);
            if (mission.exists() && Boolean.TRUE.equals(mission.getBoolean(key))) return null;
            DocumentSnapshot user = transaction.get(userRef);
            if (!user.exists()) throw new IllegalStateException("Korisnicki profil ne postoji.");
            Map<String,Object> values = new HashMap<>();
            boolean allDone = true;
            for (String candidate : KEYS) {
                boolean done = candidate.equals(key) || (mission.exists() && Boolean.TRUE.equals(mission.getBoolean(candidate)));
                values.put(candidate, done); allDone &= done;
            }
            boolean alreadyRewarded = mission.exists() && Boolean.TRUE.equals(mission.getBoolean("allRewarded"));
            values.put("allRewarded", alreadyRewarded || allDone);
            values.put("updatedAt", FieldValue.serverTimestamp());
            long reward = 3 + (allDone && !alreadyRewarded ? 3 : 0);
            long stars = number(user.getLong("stars")) + reward;
            StarTokenProgress.Result progress = StarTokenProgress.apply(
                    number(user.getLong("starTokenProgress")), reward);
            Map<String,Object> userValues = new HashMap<>();
            userValues.put("stars", stars); userValues.put("league", LeaguePolicy.leagueForStars(stars));
            userValues.put("weeklyStars", number(user.getLong("weeklyStars")) + reward);
            userValues.put("monthlyStars", number(user.getLong("monthlyStars")) + reward);
            userValues.put("starTokenProgress", progress.remainingProgress);
            userValues.put("tokens", number(user.getLong("tokens")) + progress.earnedTokens
                    + (allDone && !alreadyRewarded ? 2 : 0));
            userValues.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(missionRef, values, SetOptions.merge());
            transaction.update(userRef, userValues);
            return null;
        });
    }

    private long number(Long value) { return value == null ? 0 : value; }
}
