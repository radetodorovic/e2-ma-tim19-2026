package com.example.mobilnekt1.auth.data;

public interface AuthCallback {
    void onSuccess();

    void onVerificationRequired();

    void onError(String message);
}
