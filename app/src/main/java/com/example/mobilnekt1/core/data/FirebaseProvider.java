package com.example.mobilnekt1.core.data;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public final class FirebaseProvider {
    private static FirebaseProvider instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private FirebaseProvider(Context context) {
        FirebaseAuth authInstance = null;
        FirebaseFirestore firestoreInstance = null;
        try {
            FirebaseApp app = FirebaseApp.initializeApp(context.getApplicationContext());
            if (app != null) {
                authInstance = FirebaseAuth.getInstance(app);
                firestoreInstance = FirebaseFirestore.getInstance(app);
            }
        } catch (IllegalStateException ignored) {
            // Repositories expose a configuration error to the UI.
        }
        auth = authInstance;
        firestore = firestoreInstance;
    }

    public static synchronized FirebaseProvider getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseProvider(context);
        }
        return instance;
    }

    public boolean isConfigured() {
        return auth != null && firestore != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth == null ? null : auth.getCurrentUser();
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    private FirebaseProvider() {
        auth = null;
        firestore = null;
    }
}
