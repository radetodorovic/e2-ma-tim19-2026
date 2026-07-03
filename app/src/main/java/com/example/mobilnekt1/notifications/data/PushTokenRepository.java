package com.example.mobilnekt1.notifications.data;

import android.content.Context;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.messaging.FirebaseMessaging;

public final class PushTokenRepository {
    private final FirebaseProvider firebase;
    public PushTokenRepository(Context context) { firebase = FirebaseProvider.getInstance(context); }

    public void refresh() {
        if (firebase.getCurrentUser() == null) return;
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::save);
    }

    public void save(String token) {
        if (firebase.getCurrentUser() == null || token == null || token.isEmpty()) return;
        firebase.getFirestore().collection("users").document(firebase.getCurrentUser().getUid())
                .update("fcmTokens", FieldValue.arrayUnion(token));
    }
}
