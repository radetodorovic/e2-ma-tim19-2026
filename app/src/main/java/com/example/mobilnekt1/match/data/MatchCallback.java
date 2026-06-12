package com.example.mobilnekt1.match.data;

public interface MatchCallback {
    void onSuccess(String matchId);

    void onError(String message);
}
