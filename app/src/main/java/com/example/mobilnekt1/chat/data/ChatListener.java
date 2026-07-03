package com.example.mobilnekt1.chat.data;

import com.example.mobilnekt1.chat.domain.ChatMessage;
import java.util.List;

public interface ChatListener {
    void onReady(String region);
    void onChanged(List<ChatMessage> messages);
    void onError(String message);
}
