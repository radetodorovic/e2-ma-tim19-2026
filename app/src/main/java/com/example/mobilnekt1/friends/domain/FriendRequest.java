package com.example.mobilnekt1.friends.domain;

import com.google.firebase.Timestamp;

public final class FriendRequest {
    public String id;
    public String senderId;
    public String senderUsername;
    public String receiverId;
    public String receiverUsername;
    public String status;
    public Timestamp createdAt;
    public Timestamp updatedAt;

    public FriendRequest() {
    }
}
