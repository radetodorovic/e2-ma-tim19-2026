package com.example.mobilnekt1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import com.example.mobilnekt1.chat.data.*;
import com.example.mobilnekt1.chat.domain.ChatMessage;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import java.text.DateFormat;
import java.util.*;

public final class ChatActivity extends BaseKt1Activity {
    private ChatRepository repository;
    private LinearLayout container;
    private ScrollView scroll;
    private EditText input;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_chat);
        container = findViewById(R.id.container_chat_messages); scroll = findViewById(R.id.scroll_chat); input = findViewById(R.id.input_chat_message);
        repository = new ChatRepository(this);
        repository.listen(new ChatListener() {
            @Override public void onReady(String region) { ((TextView)findViewById(R.id.text_chat_region)).setText(region); }
            @Override public void onChanged(List<ChatMessage> messages) { render(messages); }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.chat_title), message); }
        });
        findViewById(R.id.button_send_chat).setOnClickListener(v -> repository.send(input.getText().toString(), new GameActionCallback() {
            @Override public void onSuccess() { input.setText(""); }
            @Override public void onError(String message) { input.setError(message); }
        }));
    }

    private void render(List<ChatMessage> messages) {
        container.removeAllViews(); DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (ChatMessage message : messages) {
            TextView row = new TextView(this); String time = message.createdAt == null ? "" : format.format(message.createdAt.toDate());
            row.setText(message.senderName + " · " + time + "\n" + message.text); row.setTextSize(16); row.setTextColor(Color.WHITE);
            row.setPadding(18,12,18,12); row.setBackgroundColor(getColor(message.mine ? R.color.accent : R.color.primary));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2,-2); params.gravity = message.mine ? Gravity.END : Gravity.START;
            params.setMargins(8,6,8,6); row.setLayoutParams(params); container.addView(row);
        }
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override protected void onDestroy() { if (repository != null) repository.stop(); super.onDestroy(); }
}
