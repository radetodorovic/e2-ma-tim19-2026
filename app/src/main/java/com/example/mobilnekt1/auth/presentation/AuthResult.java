package com.example.mobilnekt1.auth.presentation;

public final class AuthResult {
    public enum Status {
        SUCCESS,
        VERIFICATION_REQUIRED,
        ERROR
    }

    public final Status status;
    public final String message;

    private AuthResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static AuthResult success() {
        return new AuthResult(Status.SUCCESS, null);
    }

    public static AuthResult verificationRequired() {
        return new AuthResult(Status.VERIFICATION_REQUIRED, null);
    }

    public static AuthResult error(String message) {
        return new AuthResult(Status.ERROR, message);
    }
}
