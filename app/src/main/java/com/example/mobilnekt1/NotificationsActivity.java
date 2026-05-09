package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NotificationsActivity extends BaseKt1Activity {
    private LinearLayout notificationsContainer;
    private boolean showUnreadOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationsContainer = findViewById(R.id.container_notifications);
        Button allButton = findViewById(R.id.button_all_notifications);
        Button unreadButton = findViewById(R.id.button_unread_notifications);
        Button channelsButton = findViewById(R.id.button_notification_channels);

        allButton.setOnClickListener(v -> {
            showUnreadOnly = false;
            renderNotifications();
        });
        unreadButton.setOnClickListener(v -> {
            showUnreadOnly = true;
            renderNotifications();
        });
        channelsButton.setOnClickListener(v -> showInfoDialog(
                getString(R.string.notification_channels_title),
                "KT1 mock kanali:\n- Cet\n- Rangiranje\n- Nagrade\n- Ostalo"
        ));
        renderNotifications();
    }

    private void renderNotifications() {
        notificationsContainer.removeAllViews();
        for (MockStudentThreeData.NotificationItem item : MockStudentThreeData.NOTIFICATIONS) {
            if (showUnreadOnly && item.read) {
                continue;
            }
            TextView row = new TextView(this);
            row.setText(item.channel + " | " + item.time
                    + "\n" + item.message
                    + "\nStatus: " + (item.read ? "procitana" : "neprocitana")
                    + "\nKlik oznacava kao procitano.");
            row.setTextColor(getResources().getColor(R.color.text_primary));
            row.setTextSize(16);
            row.setBackgroundResource(R.drawable.card_background);
            row.setPadding(16, 16, 16, 16);
            row.setOnClickListener(v -> {
                item.read = true;
                showToast(R.string.notification_marked_read);
                renderNotifications();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 12);
            notificationsContainer.addView(row, params);
        }
        if (notificationsContainer.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_notifications);
            empty.setTextColor(getResources().getColor(R.color.text_secondary));
            empty.setTextSize(16);
            notificationsContainer.addView(empty);
        }
    }
}
