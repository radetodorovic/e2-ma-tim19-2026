package com.example.mobilnekt1.notifications.presentation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.content.pm.PackageManager;

import com.example.mobilnekt1.NotificationsActivity;
import com.example.mobilnekt1.notifications.domain.NotificationItem;

public final class NotificationChannels {
    public static final String CHAT = "chat";
    public static final String RANKING = "ranking";
    public static final String REWARDS = "rewards";
    public static final String OTHER = "other";

    private NotificationChannels() { }

    public static void create(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHAT, "Cet", NotificationManager.IMPORTANCE_HIGH));
        manager.createNotificationChannel(new NotificationChannel(RANKING, "Rangiranje", NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(new NotificationChannel(REWARDS, "Nagrade", NotificationManager.IMPORTANCE_HIGH));
        manager.createNotificationChannel(new NotificationChannel(OTHER, "Ostalo", NotificationManager.IMPORTANCE_DEFAULT));
    }

    public static void show(Context context, NotificationItem item) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, NotificationsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, item.id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, validChannel(item.channel)) : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(item.title).setContentText(item.message)
                .setAutoCancel(true).setContentIntent(pendingIntent);
        manager.notify(item.id.hashCode(), builder.build());
    }

    private static String validChannel(String channel) {
        if (CHAT.equals(channel) || RANKING.equals(channel) || REWARDS.equals(channel)) return channel;
        return OTHER;
    }
}
