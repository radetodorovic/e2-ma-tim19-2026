package com.example.mobilnekt1.auth.data;

import android.content.Context;
import android.util.Patterns;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.example.mobilnekt1.auth.domain.AuthValidator;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.data.UserRepository;

public final class FirebaseAuthRepository {
    private static FirebaseAuthRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final SessionManager sessionManager;
    private final UserRepository userRepository;
    private final boolean configured;

    private FirebaseAuthRepository(Context context) {
        FirebaseAuth authInstance = null;
        FirebaseFirestore firestoreInstance = null;
        boolean initialized = false;
        try {
            FirebaseApp app = FirebaseApp.initializeApp(context);
            if (app != null) {
                authInstance = FirebaseAuth.getInstance(app);
                firestoreInstance = FirebaseFirestore.getInstance(app);
                initialized = true;
            }
        } catch (IllegalStateException ignored) {
            // The UI reports the missing google-services.json configuration.
        }
        auth = authInstance;
        firestore = firestoreInstance;
        configured = initialized;
        sessionManager = new SessionManager(context);
        userRepository = new UserRepository(context);
    }

    public static synchronized FirebaseAuthRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void register(String email, String username, String region, String password,
                         AuthCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        String cleanUsername = username.trim();
        String normalizedUsername = AuthValidator.normalizeUsername(username);

        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError("Kreiranje korisnika nije uspelo.");
                        return;
                    }
                    UserProfileChangeRequest profileChange = new UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanUsername)
                            .build();
                    user.updateProfile(profileChange).addOnCompleteListener(task ->
                            completeRegistration(user, cleanEmail, cleanUsername,
                                    normalizedUsername, region.trim(), callback));
                })
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    private void completeRegistration(FirebaseUser user, String email, String username,
                                      String normalizedUsername, String region,
                                      AuthCallback callback) {
        DocumentReference usernameRef = firestore.collection("usernames").document(normalizedUsername);
        DocumentReference userRef = firestore.collection("users").document(user.getUid());
        firestore.runTransaction(transaction -> {
            DocumentSnapshot usernameSnapshot = transaction.get(usernameRef);
            if (usernameSnapshot.exists()) {
                throw new FirebaseFirestoreException("Korisnicko ime je zauzeto.",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            Map<String, Object> usernameData = new HashMap<>();
            usernameData.put("uid", user.getUid());
            usernameData.put("email", email);
            transaction.set(usernameRef, usernameData);

            Map<String, Object> profile = new HashMap<>();
            profile.put("email", email);
            profile.put("uid", user.getUid());
            profile.put("username", username);
            profile.put("usernameNormalized", normalizedUsername);
            profile.put("region", region);
            profile.put("tokens", 5);
            profile.put("stars", 0);
            profile.put("weeklyStars", 0);
            profile.put("monthlyStars", 0);
            profile.put("starTokenProgress", 0);
            profile.put("league", 0);
            profile.put("avatarId", UserRepository.DEFAULT_AVATAR_ID);
            profile.put("avatarFrame", UserRepository.DEFAULT_AVATAR_FRAME);
            profile.put("qrCodeValue", "slagalica:user:" + user.getUid());
            profile.put("isOnline", false);
            profile.put("inGame", false);
            profile.put("activeMatchId", null);
            profile.put("lastSettledMatchId", null);
            profile.put("lastDailyTokenClaimAt", FieldValue.serverTimestamp());
            profile.put("createdAt", FieldValue.serverTimestamp());
            profile.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(userRef, profile);
            return null;
        }).addOnSuccessListener(unused -> user.sendEmailVerification()
                .addOnSuccessListener(result -> callback.onVerificationRequired())
                .addOnFailureListener(error -> callback.onError(
                        "Nalog je kreiran, ali email nije poslat. Prijavite se i posaljite ga ponovo.")))
                .addOnFailureListener(error -> user.delete().addOnCompleteListener(task -> {
                    auth.signOut();
                    callback.onError(messageFor(error));
                }));
    }

    public void login(String identifier, String password, AuthCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        String cleanIdentifier = identifier.trim();
        if (Patterns.EMAIL_ADDRESS.matcher(cleanIdentifier).matches()) {
            signIn(cleanIdentifier.toLowerCase(Locale.ROOT), password, callback);
            return;
        }
        firestore.collection("usernames")
                .document(AuthValidator.normalizeUsername(cleanIdentifier))
                .get()
                .addOnSuccessListener(snapshot -> {
                    String email = snapshot.getString("email");
                    if (!snapshot.exists() || email == null) {
                        callback.onError("Korisnik sa tim korisnickim imenom ne postoji.");
                        return;
                    }
                    signIn(email, password, callback);
                })
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    private void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError("Prijava nije uspela.");
                        return;
                    }
                    user.reload().addOnCompleteListener(task -> {
                        FirebaseUser refreshedUser = auth.getCurrentUser();
                        if (refreshedUser == null || !refreshedUser.isEmailVerified()) {
                            callback.onVerificationRequired();
                            return;
                        }
                        loadSession(refreshedUser, callback);
                    });
                })
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    private void loadSession(FirebaseUser user, AuthCallback callback) {
        userRepository.ensureCurrentUserDefaults(true, new GameActionCallback() {
            @Override public void onSuccess() {
                firestore.collection("users").document(user.getUid()).get()
                        .addOnSuccessListener(snapshot -> {
                            String username = snapshot.getString("username");
                            sessionManager.save(user.getUid(), user.getEmail(),
                                    username == null ? "" : username);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(error -> callback.onError(messageFor(error)));
            }

            @Override public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void checkEmailVerification(AuthCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla. Prijavite se ponovo.");
            return;
        }
        user.reload().addOnSuccessListener(result -> {
            FirebaseUser refreshedUser = auth.getCurrentUser();
            if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                loadSession(refreshedUser, callback);
            } else {
                callback.onVerificationRequired();
            }
        }).addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    public void resendVerification(AuthCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla. Prijavite se ponovo.");
            return;
        }
        user.sendEmailVerification()
                .addOnSuccessListener(result -> callback.onVerificationRequired())
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    public void sendPasswordReset(String email, AuthCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        auth.sendPasswordResetEmail(email.trim().toLowerCase(Locale.ROOT))
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    public void changePassword(String oldPassword, String newPassword, AuthCallback callback) {
        if (!ensureConfigured(callback)) {
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onError("Za promenu lozinke morate biti prijavljeni.");
            return;
        }
        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), oldPassword))
                .addOnSuccessListener(result -> user.updatePassword(newPassword)
                        .addOnSuccessListener(unused -> callback.onSuccess())
                        .addOnFailureListener(error -> callback.onError(messageFor(error))))
                .addOnFailureListener(error -> callback.onError(messageFor(error)));
    }

    public boolean hasVerifiedUser() {
        return configured && auth.getCurrentUser() != null && auth.getCurrentUser().isEmailVerified()
                && sessionManager.isLoggedIn();
    }

    public boolean hasUserAwaitingVerification() {
        return configured && auth.getCurrentUser() != null && !auth.getCurrentUser().isEmailVerified();
    }

    public void logout() {
        sessionManager.clear();
        if (!configured || auth.getCurrentUser() == null) {
            if (configured) auth.signOut();
            return;
        }
        userRepository.updateUserOnlineStatus(false, new GameActionCallback() {
            @Override public void onSuccess() { auth.signOut(); }
            @Override public void onError(String message) { auth.signOut(); }
        });
    }

    private boolean ensureConfigured(AuthCallback callback) {
        if (!configured) {
            callback.onError("Firebase nije konfigurisan. Dodajte app/google-services.json.");
        }
        return configured;
    }

    private String messageFor(Exception error) {
        if (error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode()
                == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
            return "Korisnicko ime je zauzeto.";
        }
        if (error instanceof FirebaseFirestoreException) {
            return "Pristup bazi nije uspeo. Pokusajte ponovo.";
        }
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            switch (code) {
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Email adresa je vec registrovana.";
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_USER_NOT_FOUND":
                    return "Email/korisnicko ime ili lozinka nisu ispravni.";
                case "ERROR_USER_DISABLED":
                    return "Ovaj korisnicki nalog je onemogucen.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Previse pokusaja. Pokusajte ponovo kasnije.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Nema internet konekcije.";
                case "ERROR_REQUIRES_RECENT_LOGIN":
                    return "Ponovo se prijavite pre promene lozinke.";
                default:
                    break;
            }
        }
        String message = error.getLocalizedMessage();
        return message == null ? "Doslo je do neocekivane greske." : message;
    }
}
