package com.example.mobilnekt1.regions.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.regions.domain.RegionCatalog;
import com.example.mobilnekt1.regions.domain.RegionStats;
import com.example.mobilnekt1.regions.domain.RegionPlayerPoint;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.*;

public final class RegionsRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration usersRegistration;

    public RegionsRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void listen(RegionsListener listener) {
        usersRegistration = firebase.getFirestore().collection("users").addSnapshotListener((users, error) -> {
            if (error != null || users == null) { listener.onError("Podaci o regionima nisu dostupni."); return; }
            Map<String, RegionStats> values = new LinkedHashMap<>();
            for (String name : RegionCatalog.NAMES) { RegionStats stats = new RegionStats(); stats.name = name; values.put(name, stats); }
            String currentRegion = null;
            List<RegionPlayerPoint> points = new ArrayList<>();
            String uid = firebase.getCurrentUser() == null ? null : firebase.getCurrentUser().getUid();
            for (DocumentSnapshot user : users.getDocuments()) {
                if (Boolean.TRUE.equals(user.getBoolean("isGuest"))) continue;
                String region = RegionCatalog.canonical(user.getString("region"));
                if (region == null) continue;
                RegionStats stats = values.get(region);
                stats.registeredPlayers++;
                if (Boolean.TRUE.equals(user.getBoolean("isOnline"))) stats.activePlayers++;
                Long stars = user.getLong("monthlyStars"); stats.monthlyStars += stars == null ? 0 : stars;
                if (user.getId().equals(uid)) currentRegion = region;
                Double latitude = user.getDouble("regionLatitude");
                Double longitude = user.getDouble("regionLongitude");
                if (latitude != null && longitude != null) {
                    RegionPlayerPoint point = new RegionPlayerPoint();
                    point.latitude = latitude; point.longitude = longitude;
                    point.currentUser = user.getId().equals(uid); points.add(point);
                }
            }
            String selectedRegion = currentRegion;
            firebase.getFirestore().collection("regionStats").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot history : task.getResult().getDocuments()) {
                        RegionStats stats = values.get(history.getId());
                        if (stats == null) continue;
                        stats.firstPlaces = (int) number(history, "firstPlaces");
                        stats.secondPlaces = (int) number(history, "secondPlaces");
                        stats.thirdPlaces = (int) number(history, "thirdPlaces");
                    }
                }
                List<RegionStats> result = new ArrayList<>(values.values());
                Collections.sort(result, (a, b) -> Long.compare(b.monthlyStars, a.monthlyStars));
                listener.onChanged(result, points, selectedRegion);
            });
        });
    }

    public void stop() { if (usersRegistration != null) usersRegistration.remove(); usersRegistration = null; }

    private long number(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value;
    }
}
