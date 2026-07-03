package com.example.mobilnekt1.chat.domain;

import com.google.firebase.Timestamp;

public final class ChatMessage {
    public String senderId;
    public String senderName;
    public String text;
    public Timestamp createdAt;
    public boolean mine;
}
