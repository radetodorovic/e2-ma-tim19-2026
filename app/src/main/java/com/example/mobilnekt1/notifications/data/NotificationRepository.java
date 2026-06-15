package com.example.mobilnekt1.notifications.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.notifications.domain.NotificationItem;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NotificationRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration registration;

    public NotificationRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void initialize(GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) { callback.onError("Sesija je istekla."); return; }
        collection().limit(1).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.isEmpty()) { callback.onSuccess(); return; }
            Map<String, Object> data = new HashMap<>();
            data.put("channel", "other");
            data.put("title", "Notifikacije su aktivne");
            data.put("message", "Ovde ce se cuvati istorija sistemskih obavestenja.");
            data.put("action", "notifications");
            data.put("read", false);
            data.put("createdAt", FieldValue.serverTimestamp());
            collection().add(data).addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(error -> callback.onError(message(error)));
        }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void listen(NotificationListener listener) {
        stopListening();
        if (firebase.getCurrentUser() == null) { listener.onError("Sesija je istekla."); return; }
        registration = collection().orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { listener.onError(message(error)); return; }
                    List<NotificationItem> items = new ArrayList<>();
                    if (snapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                            NotificationItem item = document.toObject(NotificationItem.class);
                            item.id = document.getId();
                            items.add(item);
                        }
                    }
                    listener.onChanged(items);
                });
    }

    public void markRead(String notificationId, GameActionCallback callback) {
        collection().document(notificationId).update("read", true)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void saveIncoming(String channel, String title, String message, String action,
                             GameActionCallback callback) {
        if (firebase.getCurrentUser() == null) { callback.onError("Korisnik nije prijavljen."); return; }
        Map<String, Object> data = new HashMap<>();
        data.put("channel", channel);
        data.put("title", title);
        data.put("message", message);
        data.put("action", action);
        data.put("read", false);
        data.put("createdAt", FieldValue.serverTimestamp());
        collection().add(data).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private com.google.firebase.firestore.CollectionReference collection() {
        return firebase.getFirestore().collection("users").document(firebase.getCurrentUser().getUid())
                .collection("notifications");
    }
    public void stopListening() { if (registration != null) { registration.remove(); registration = null; } }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Notifikacije nisu dostupne." : error.getLocalizedMessage(); }
}
