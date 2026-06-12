package com.example.mobilnekt1.auth.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {
    private static final String FILE_NAME = "auth_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public void save(String uid, String email, String username) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_UID, uid)
                .putString(KEY_EMAIL, email)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
