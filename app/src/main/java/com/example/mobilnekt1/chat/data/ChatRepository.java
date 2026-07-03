package com.example.mobilnekt1.chat.data;

import android.content.Context;
import com.example.mobilnekt1.chat.domain.ChatMessage;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.missions.data.ClientMissionRepository;
import com.google.firebase.firestore.*;
import java.util.*;

public final class ChatRepository {
    private final FirebaseProvider firebase;
    private ListenerRegistration registration;
    private String region, username;
    private final Context context;

    public ChatRepository(Context context) { this.context = context.getApplicationContext(); firebase = FirebaseProvider.getInstance(context); }

    public void listen(ChatListener listener) {
        if (firebase.getCurrentUser() == null) { listener.onError("Sesija je istekla."); return; }
        String uid = firebase.getCurrentUser().getUid();
        firebase.getFirestore().collection("users").document(uid).get().addOnSuccessListener(user -> {
            region = user.getString("region"); username = user.getString("username");
            if (region == null || username == null) { listener.onError("Profil nema region."); return; }
            listener.onReady(region);
            registration = messages().orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(100)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) { listener.onError(error.getLocalizedMessage()); return; }
                        List<ChatMessage> result = new ArrayList<>();
                        if (snapshot != null) for (DocumentSnapshot document : snapshot.getDocuments()) {
                            ChatMessage message = document.toObject(ChatMessage.class);
                            if (message != null) { message.mine = uid.equals(message.senderId); result.add(message); }
                        }
                        listener.onChanged(result);
                    });
        }).addOnFailureListener(error -> listener.onError(error.getLocalizedMessage()));
    }

    public void send(String text, GameActionCallback callback) {
        if (region == null || firebase.getCurrentUser() == null) { callback.onError("Cet nije spreman."); return; }
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty() || clean.length() > 500) { callback.onError("Poruka mora imati od 1 do 500 znakova."); return; }
        Map<String,Object> value = new HashMap<>();
        value.put("senderId", firebase.getCurrentUser().getUid()); value.put("senderName", username);
        value.put("text", clean); value.put("createdAt", FieldValue.serverTimestamp());
        messages().add(value).addOnSuccessListener(unused -> {
                    createChatNotifications(clean);
                    new ClientMissionRepository(context).complete(ClientMissionRepository.SEND_CHAT);
                    callback.onSuccess();
                })
                .addOnFailureListener(error -> callback.onError(error.getLocalizedMessage()));
    }

    public void stop() { if (registration != null) registration.remove(); registration = null; }

    private void createChatNotifications(String text) {
        String senderId = firebase.getCurrentUser().getUid();
        firebase.getFirestore().collection("users").whereEqualTo("region", region).get()
                .addOnSuccessListener(users -> {
                    WriteBatch batch = firebase.getFirestore().batch();
                    for (DocumentSnapshot user : users.getDocuments()) {
                        if (user.getId().equals(senderId) || Boolean.TRUE.equals(user.getBoolean("isGuest"))) continue;
                        DocumentReference notification = user.getReference().collection("notifications").document();
                        Map<String, Object> data = new HashMap<>();
                        data.put("senderId", senderId); data.put("channel", "chat");
                        data.put("title", "Nova poruka - " + region);
                        data.put("message", username + ": " + text); data.put("action", "chat");
                        data.put("read", false); data.put("createdAt", FieldValue.serverTimestamp());
                        batch.set(notification, data);
                    }
                    batch.commit();
                });
    }
    private CollectionReference messages() { return firebase.getFirestore().collection("regionalChats").document(region).collection("messages"); }
}
