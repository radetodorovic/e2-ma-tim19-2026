package com.example.mobilnekt1.ranking.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.ranking.domain.RankingEntry;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RankingRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration registration;

    public RankingRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void listen(RankingListener listener) {
        registration = firebase.getFirestore().collection("users").addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null) {
                listener.onError(error == null ? "Rang-lista nije dostupna." : error.getLocalizedMessage());
                return;
            }
            List<RankingEntry> weekly = new ArrayList<>();
            List<RankingEntry> monthly = new ArrayList<>();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                if (Boolean.TRUE.equals(document.getBoolean("isGuest"))) continue;
                if (number(document, "weeklyMatches") > 0) weekly.add(entry(document, "weeklyStars"));
                if (number(document, "monthlyMatches") > 0) monthly.add(entry(document, "monthlyStars"));
            }
            java.util.Comparator<RankingEntry> order = (a, b) -> {
                int stars = Long.compare(b.stars, a.stars);
                return stars != 0 ? stars : a.username.compareToIgnoreCase(b.username);
            };
            Collections.sort(weekly, order);
            Collections.sort(monthly, order);
            listener.onChanged(weekly, monthly);
        });
    }

    public void stop() { if (registration != null) registration.remove(); registration = null; }

    private RankingEntry entry(DocumentSnapshot document, String field) {
        RankingEntry value = new RankingEntry();
        value.username = document.getString("username");
        if (value.username == null) value.username = "-";
        value.stars = number(document, field);
        value.league = number(document, "league");
        return value;
    }

    private long number(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value;
    }
}
