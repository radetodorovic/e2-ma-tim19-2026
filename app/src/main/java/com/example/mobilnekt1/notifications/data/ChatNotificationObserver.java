package com.example.mobilnekt1.notifications.data;

import android.content.Context;
import com.example.mobilnekt1.SlagalicaApplication;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.notifications.domain.NotificationItem;
import com.example.mobilnekt1.notifications.presentation.NotificationChannels;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.HashSet;
import java.util.Set;

public final class ChatNotificationObserver {
    private static ListenerRegistration registration;
    private static boolean initialized;
    private static final Set<String> knownIds = new HashSet<>();

    private ChatNotificationObserver() { }

    public static synchronized void start(Context context) {
        if (registration != null) return;
        Context appContext = context.getApplicationContext();
        FirebaseProvider firebase = FirebaseProvider.getInstance(appContext);
        if (firebase.getCurrentUser() == null || firebase.getCurrentUser().isAnonymous()) return;
        String uid = firebase.getCurrentUser().getUid();
        registration = firebase.getFirestore().collection("users").document(uid)
                .collection("notifications").orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50).addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    if (!initialized) {
                        for (com.google.firebase.firestore.DocumentSnapshot document
                                : snapshot.getDocuments()) {
                            knownIds.add(document.getId());
                        }
                        initialized = true;
                        return;
                    }
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED
                                || !knownIds.add(change.getDocument().getId())) continue;
                        NotificationItem item = change.getDocument().toObject(NotificationItem.class);
                        item.id = change.getDocument().getId();
                        if (!NotificationChannels.CHAT.equals(item.channel)) continue;
                        SlagalicaApplication application = (SlagalicaApplication) appContext;
                        if (!application.isInForeground()) {
                            NotificationChannels.create(appContext);
                            NotificationChannels.show(appContext, item);
                        }
                    }
                });
    }

    public static synchronized void stop() {
        if (registration != null) registration.remove();
        registration = null; initialized = false; knownIds.clear();
    }
}
