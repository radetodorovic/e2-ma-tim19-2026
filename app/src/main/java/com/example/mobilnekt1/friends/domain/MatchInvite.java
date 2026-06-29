package com.example.mobilnekt1.friends.domain;

import com.google.firebase.Timestamp;

public final class MatchInvite {
    public String id;
    public String matchId;
    public String senderId;
    public String senderUsername;
    public String receiverId;
    public String receiverUsername;
    public String status;
    public Timestamp createdAt;
    public Timestamp expiresAt;
    public Timestamp updatedAt;

    public MatchInvite() {
    }

    public boolean isExpired(long nowMillis) {
        return expiresAt != null && expiresAt.toDate().getTime() <= nowMillis;
    }
}
