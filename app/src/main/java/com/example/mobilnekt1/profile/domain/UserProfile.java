package com.example.mobilnekt1.profile.domain;

import com.google.firebase.Timestamp;

public final class UserProfile {
    public String uid;
    public String username;
    public String email;
    public String region;
    public String avatarId;
    public String avatarFrame;
    public String qrCodeValue;
    public long tokens;
    public long stars;
    public long weeklyStars;
    public long monthlyStars;
    public long league;
    public boolean isOnline;
    public boolean inGame;
    public Timestamp lastDailyTokenClaimAt;
    public Timestamp createdAt;
    public Timestamp updatedAt;

    public UserProfile() {
    }
}
