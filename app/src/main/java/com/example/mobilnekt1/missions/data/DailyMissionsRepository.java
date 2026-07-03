package com.example.mobilnekt1.missions.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.missions.domain.DailyMissions;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.*;

public final class DailyMissionsRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration registration;
    public DailyMissionsRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void listen(DailyMissionsListener listener) {
        if (firebase.getCurrentUser() == null) { listener.onError("Sesija je istekla."); return; }
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        registration = firebase.getFirestore().collection("users").document(firebase.getCurrentUser().getUid())
                .collection("dailyMissions").document(day).addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(error.getLocalizedMessage()); return; }
                    DailyMissions value = snapshot != null && snapshot.exists()
                            ? snapshot.toObject(DailyMissions.class) : new DailyMissions();
                    listener.onChanged(value == null ? new DailyMissions() : value);
                });
    }
    public void stop() { if (registration != null) registration.remove(); registration = null; }
}
