package com.example.mobilnekt1.notifications.data;

import com.example.mobilnekt1.notifications.domain.NotificationItem;
import com.example.mobilnekt1.notifications.presentation.NotificationChannels;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.UUID;

public final class SlagalicaMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        new PushTokenRepository(this).save(token);
    }

    @Override public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String channel = value(data.get("channel"), NotificationChannels.OTHER);
        String title = value(data.get("title"), remoteMessage.getNotification() == null
                ? "Slagalica" : remoteMessage.getNotification().getTitle());
        String message = value(data.get("message"), remoteMessage.getNotification() == null
                ? "Novo obavestenje" : remoteMessage.getNotification().getBody());
        String action = value(data.get("action"), "notifications");
        NotificationItem item = new NotificationItem();
        item.id = UUID.randomUUID().toString(); item.channel = channel;
        item.title = title; item.message = message; item.action = action;
        NotificationChannels.create(this);
        NotificationChannels.show(this, item);
    }

    private String value(String candidate, String fallback) {
        return candidate == null || candidate.trim().isEmpty() ? fallback : candidate;
    }
}
