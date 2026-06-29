package com.example.mobilnekt1.profile.data;

public interface DailyTokenCallback {
    void onComplete(boolean claimed, long amount);

    void onError(String message);
}
