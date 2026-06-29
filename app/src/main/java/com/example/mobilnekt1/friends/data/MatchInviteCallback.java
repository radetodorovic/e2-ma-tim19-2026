package com.example.mobilnekt1.friends.data;

public interface MatchInviteCallback {
    void onSuccess(String matchId, String inviteId);

    void onError(String message);
}
