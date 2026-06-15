package com.example.mobilnekt1.notifications.data;

import com.example.mobilnekt1.notifications.domain.NotificationItem;
import java.util.List;

public interface NotificationListener {
    void onChanged(List<NotificationItem> notifications);
    void onError(String message);
}
