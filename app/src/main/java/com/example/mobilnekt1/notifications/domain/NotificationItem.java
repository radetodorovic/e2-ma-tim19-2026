package com.example.mobilnekt1.notifications.domain;

import com.google.firebase.Timestamp;

public final class NotificationItem {
    public String id;
    public String channel;
    public String title;
    public String message;
    public String action;
    public boolean read;
    public Timestamp createdAt;

    public NotificationItem() { }
}
