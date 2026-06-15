package com.example.mobilnekt1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.notifications.data.NotificationListener;
import com.example.mobilnekt1.notifications.data.NotificationRepository;
import com.example.mobilnekt1.notifications.domain.NotificationItem;
import com.example.mobilnekt1.notifications.presentation.NotificationChannels;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NotificationsActivity extends BaseKt1Activity {
    private enum Filter { ALL, READ, UNREAD }
    private final List<NotificationItem> notifications = new ArrayList<>();
    private final Set<String> announced = new HashSet<>();
    private NotificationRepository repository;
    private LinearLayout container;
    private Filter filter = Filter.ALL;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        NotificationChannels.create(this);
        requestPermission();
        container = findViewById(R.id.container_notifications);
        findViewById(R.id.button_all_notifications).setOnClickListener(v -> { filter = Filter.ALL; render(); });
        findViewById(R.id.button_read_notifications).setOnClickListener(v -> { filter = Filter.READ; render(); });
        findViewById(R.id.button_unread_notifications).setOnClickListener(v -> { filter = Filter.UNREAD; render(); });
        findViewById(R.id.button_notification_channels).setOnClickListener(v -> showInfoDialog(
                getString(R.string.notification_channels_title), "Cet\nRangiranje\nNagrade\nOstalo"));
        repository = new NotificationRepository(this);
        repository.listen(new NotificationListener() {
            @Override public void onChanged(List<NotificationItem> items) {
                notifications.clear(); notifications.addAll(items);
                for (NotificationItem item : items) {
                    if (!item.read && announced.add(item.id) && canNotify()) NotificationChannels.show(NotificationsActivity.this, item);
                }
                render();
            }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.match_error_title), message); }
        });
        repository.initialize(callback());
    }

    private void render() {
        container.removeAllViews();
        for (NotificationItem item : notifications) {
            if (filter == Filter.READ && !item.read || filter == Filter.UNREAD && item.read) continue;
            TextView row = new TextView(this);
            String time = item.createdAt == null ? "" : DateFormat.getDateTimeInstance().format(item.createdAt.toDate());
            row.setText(item.title + "\n" + item.message + "\n" + time + "\nStatus: "
                    + (item.read ? "procitana" : "neprocitana"));
            row.setTextColor(getResources().getColor(R.color.text_primary)); row.setTextSize(16);
            row.setBackgroundResource(R.drawable.card_background); row.setPadding(16, 16, 16, 16);
            row.setOnClickListener(v -> { if (!item.read) repository.markRead(item.id, callback()); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, 0, 0, 12);
            container.addView(row, params);
        }
        if (container.getChildCount() == 0) {
            TextView empty = new TextView(this); empty.setText(R.string.no_notifications);
            empty.setTextColor(getResources().getColor(R.color.text_secondary)); container.addView(empty);
        }
    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
    }
    private boolean canNotify() { return Build.VERSION.SDK_INT < 33
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED; }
    private GameActionCallback callback() { return new GameActionCallback() {
        @Override public void onSuccess() { }
        @Override public void onError(String message) { showInfoDialog(getString(R.string.match_error_title), message); }
    }; }
    @Override protected void onDestroy() { if (repository != null) repository.stopListening(); super.onDestroy(); }
}
