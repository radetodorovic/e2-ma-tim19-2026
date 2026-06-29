package com.example.mobilnekt1.friends.data;

import android.content.Context;

import com.example.mobilnekt1.auth.domain.AuthValidator;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.friends.domain.FriendProfile;
import com.example.mobilnekt1.friends.domain.FriendRequest;
import com.example.mobilnekt1.friends.domain.MatchInvite;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.match.data.MatchCallback;
import com.example.mobilnekt1.match.data.MatchCompletionRepository;
import com.example.mobilnekt1.match.data.MatchRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FriendsRepository {
    public static final long INVITE_TIMEOUT_MILLIS = 10_000L;

    private final FirebaseProvider firebase;
    private final MatchRepository matchRepository;
    private final MatchCompletionRepository completionRepository;
    private final List<ListenerRegistration> registrations = new ArrayList<>();
    private final Map<String, ListenerRegistration> profileRegistrations = new HashMap<>();
    private final Map<String, FriendProfile> friendProfiles = new HashMap<>();
    private final Map<String, Long> monthlyRanks = new HashMap<>();
    private List<FriendRequest> incomingRequests = new ArrayList<>();
    private List<FriendRequest> outgoingRequests = new ArrayList<>();
    private List<MatchInvite> incomingInvites = new ArrayList<>();
    private List<MatchInvite> outgoingInvites = new ArrayList<>();
    private FriendsListener listener;
    private String currentUserId;

    public FriendsRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
        matchRepository = new MatchRepository(context);
        completionRepository = new MatchCompletionRepository(context);
        FirebaseUser user = firebase.getCurrentUser();
        currentUserId = user == null ? null : user.getUid();
    }

    public void listen(FriendsListener listener) {
        stop();
        this.listener = listener;
        FirebaseUser user = firebase.getCurrentUser();
        if (!firebase.isConfigured() || user == null) {
            listener.onError("Sesija je istekla.");
            return;
        }
        currentUserId = user.getUid();
        listenFriendships();
        listenRequests(true);
        listenRequests(false);
        listenInvites(true);
        listenInvites(false);
        listenMonthlyRanks();
    }

    public void searchByUsername(String username, FriendSearchCallback callback) {
        if (username == null || username.trim().isEmpty()) {
            callback.onError("Unesite korisnicko ime.");
            return;
        }
        firebase.getFirestore().collection("users")
                .whereEqualTo("usernameNormalized", AuthValidator.normalizeUsername(username))
                .limit(1).get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        callback.onError("Korisnik nije pronadjen.");
                        return;
                    }
                    DocumentSnapshot document = result.getDocuments().get(0);
                    if (document.getId().equals(currentUserId)) {
                        callback.onError("Ne mozete dodati sami sebe.");
                        return;
                    }
                    callback.onSuccess(profile(document));
                }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void findByQrValue(String qrValue, FriendSearchCallback callback) {
        String prefix = "slagalica:user:";
        if (qrValue == null || !qrValue.startsWith(prefix)) {
            callback.onError("QR kod nije Slagalica korisnicki kod.");
            return;
        }
        String userId = qrValue.substring(prefix.length()).trim();
        if (userId.isEmpty() || userId.equals(currentUserId)) {
            callback.onError("QR kod ne sadrzi drugog korisnika.");
            return;
        }
        firebase.getFirestore().collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) callback.onError("Korisnik iz QR koda ne postoji.");
                    else callback.onSuccess(profile(document));
                }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void sendFriendRequest(String receiverId, GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null || receiverId == null || receiverId.equals(user.getUid())) {
            callback.onError("Zahtev nije moguce poslati.");
            return;
        }
        String pairId = pairId(user.getUid(), receiverId);
        DocumentReference requestRef = firebase.getFirestore()
                .collection("friendRequests").document(pairId);
        DocumentReference senderRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference receiverRef = firebase.getFirestore().collection("users").document(receiverId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot sender = transaction.get(senderRef);
            DocumentSnapshot receiver = transaction.get(receiverRef);
            if (!sender.exists() || !receiver.exists()) {
                throw new IllegalStateException("Korisnicki profil ne postoji.");
            }
            Map<String, Object> values = new HashMap<>();
            values.put("senderId", user.getUid());
            values.put("senderUsername", text(sender, "username"));
            values.put("receiverId", receiverId);
            values.put("receiverUsername", text(receiver, "username"));
            values.put("status", "pending");
            values.put("createdAt", FieldValue.serverTimestamp());
            values.put("updatedAt", FieldValue.serverTimestamp());
            transaction.set(requestRef, values);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void acceptFriendRequest(String requestId, GameActionCallback callback) {
        updateFriendRequest(requestId, true, callback);
    }

    public void rejectFriendRequest(String requestId, GameActionCallback callback) {
        updateRequestStatus(requestId, "rejected", callback);
    }

    public void cancelFriendRequest(String requestId, GameActionCallback callback) {
        updateRequestStatus(requestId, "cancelled", callback);
    }

    public void sendFriendlyInvite(String friendId, MatchInviteCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla.");
            return;
        }
        DocumentReference senderRef = firebase.getFirestore().collection("users").document(user.getUid());
        DocumentReference receiverRef = firebase.getFirestore().collection("users").document(friendId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot sender = transaction.get(senderRef);
            DocumentSnapshot receiver = transaction.get(receiverRef);
            if (!sender.exists() || !receiver.exists()) throw new IllegalStateException("Profil ne postoji.");
            if (Boolean.TRUE.equals(sender.getBoolean("inGame"))) {
                throw new IllegalStateException("Vec ste u partiji.");
            }
            if (!Boolean.TRUE.equals(receiver.getBoolean("isOnline"))) {
                throw new IllegalStateException("Prijatelj nije ulogovan.");
            }
            if (Boolean.TRUE.equals(receiver.getBoolean("inGame"))) {
                throw new IllegalStateException("Prijatelj je vec u partiji.");
            }
            return new String[]{text(sender, "username"), text(receiver, "username")};
        }).addOnSuccessListener(names -> matchRepository.createFriendlyMatch(new MatchCallback() {
            @Override public void onSuccess(String matchId) {
                long now = System.currentTimeMillis();
                Map<String, Object> values = new HashMap<>();
                values.put("matchId", matchId);
                values.put("senderId", user.getUid());
                values.put("senderUsername", names[0]);
                values.put("receiverId", friendId);
                values.put("receiverUsername", names[1]);
                values.put("status", "pending");
                values.put("createdAt", FieldValue.serverTimestamp());
                values.put("expiresAt", new Timestamp(new Date(now + INVITE_TIMEOUT_MILLIS)));
                values.put("updatedAt", FieldValue.serverTimestamp());
                firebase.getFirestore().collection("matchInvites").document(matchId).set(values)
                        .addOnSuccessListener(unused -> callback.onSuccess(matchId, matchId))
                        .addOnFailureListener(error -> {
                            cancelWaitingMatch(matchId);
                            callback.onError(message(error));
                        });
            }

            @Override public void onError(String message) { callback.onError(message); }
        })).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void acceptInvite(String inviteId, MatchInviteCallback callback) {
        DocumentReference inviteRef = firebase.getFirestore().collection("matchInvites").document(inviteId);
        inviteRef.get().addOnSuccessListener(invite -> {
            if (!isPendingReceiverInvite(invite)) {
                callback.onError("Poziv vise nije aktivan.");
                return;
            }
            Timestamp expiresAt = invite.getTimestamp("expiresAt");
            if (expiresAt == null || expiresAt.toDate().getTime() <= System.currentTimeMillis()) {
                expireInvite(inviteId);
                callback.onError("Poziv je istekao.");
                return;
            }
            String matchId = invite.getString("matchId");
            matchRepository.joinMatch(matchId, new MatchCallback() {
                @Override public void onSuccess(String joinedMatchId) {
                    inviteRef.update("status", "accepted", "updatedAt", FieldValue.serverTimestamp())
                            .addOnSuccessListener(unused -> callback.onSuccess(joinedMatchId, inviteId))
                            .addOnFailureListener(error -> callback.onError(message(error)));
                }

                @Override public void onError(String message) { callback.onError(message); }
            });
        }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void rejectInvite(String inviteId, GameActionCallback callback) {
        closeInvite(inviteId, "rejected", callback);
    }

    public void cancelInvite(String inviteId, GameActionCallback callback) {
        closeInvite(inviteId, "cancelled", callback);
    }

    public void expirePendingInvites() {
        long now = System.currentTimeMillis();
        for (MatchInvite invite : allInvites()) {
            if ("pending".equals(invite.status) && invite.isExpired(now)) expireInvite(invite.id);
        }
    }

    public void stop() {
        for (ListenerRegistration registration : registrations) registration.remove();
        registrations.clear();
        for (ListenerRegistration registration : profileRegistrations.values()) registration.remove();
        profileRegistrations.clear();
        friendProfiles.clear();
        listener = null;
    }

    private void listenFriendships() {
        registrations.add(firebase.getFirestore().collection("friendships")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { report(error); return; }
                    Set<String> ids = new HashSet<>();
                    if (snapshots != null) for (QueryDocumentSnapshot document : snapshots) {
                        Object value = document.get("participants");
                        if (value instanceof List) for (Object id : (List<?>) value) {
                            if (!currentUserId.equals(String.valueOf(id))) ids.add(String.valueOf(id));
                        }
                    }
                    syncProfileListeners(ids);
                }));
    }

    private void listenRequests(boolean incoming) {
        String field = incoming ? "receiverId" : "senderId";
        registrations.add(firebase.getFirestore().collection("friendRequests")
                .whereEqualTo(field, currentUserId).addSnapshotListener((snapshots, error) -> {
                    if (error != null) { report(error); return; }
                    List<FriendRequest> values = new ArrayList<>();
                    if (snapshots != null) for (QueryDocumentSnapshot document : snapshots) {
                        FriendRequest request = document.toObject(FriendRequest.class);
                        request.id = document.getId();
                        if ("pending".equals(request.status)) values.add(request);
                    }
                    if (incoming) incomingRequests = values; else outgoingRequests = values;
                    emit();
                }));
    }

    private void listenInvites(boolean incoming) {
        String field = incoming ? "receiverId" : "senderId";
        registrations.add(firebase.getFirestore().collection("matchInvites")
                .whereEqualTo(field, currentUserId).addSnapshotListener((snapshots, error) -> {
                    if (error != null) { report(error); return; }
                    List<MatchInvite> values = new ArrayList<>();
                    if (snapshots != null) for (QueryDocumentSnapshot document : snapshots) {
                        MatchInvite invite = document.toObject(MatchInvite.class);
                        invite.id = document.getId();
                        if ("pending".equals(invite.status)) values.add(invite);
                    }
                    if (incoming) incomingInvites = values; else outgoingInvites = values;
                    expirePendingInvites();
                    emit();
                }));
    }

    private void listenMonthlyRanks() {
        registrations.add(firebase.getFirestore().collection("users")
                .orderBy("monthlyStars", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { report(error); return; }
                    monthlyRanks.clear();
                    long rank = 1;
                    if (snapshots != null) for (QueryDocumentSnapshot document : snapshots) {
                        monthlyRanks.put(document.getId(), rank++);
                    }
                    for (FriendProfile profile : friendProfiles.values()) {
                        profile.monthlyRank = monthlyRanks.containsKey(profile.uid)
                                ? monthlyRanks.get(profile.uid) : 0;
                    }
                    emit();
                }));
    }

    private void syncProfileListeners(Set<String> friendIds) {
        for (String existing : new ArrayList<>(profileRegistrations.keySet())) {
            if (!friendIds.contains(existing)) {
                profileRegistrations.remove(existing).remove();
                friendProfiles.remove(existing);
            }
        }
        for (String friendId : friendIds) {
            if (profileRegistrations.containsKey(friendId)) continue;
            ListenerRegistration registration = firebase.getFirestore().collection("users")
                    .document(friendId).addSnapshotListener((document, error) -> {
                        if (error != null) { report(error); return; }
                        if (document != null && document.exists()) {
                            FriendProfile profile = profile(document);
                            profile.monthlyRank = monthlyRanks.containsKey(friendId)
                                    ? monthlyRanks.get(friendId) : 0;
                            friendProfiles.put(friendId, profile);
                            emit();
                        }
                    });
            profileRegistrations.put(friendId, registration);
        }
        emit();
    }

    private void updateFriendRequest(String requestId, boolean accept,
                                     GameActionCallback callback) {
        DocumentReference requestRef = firebase.getFirestore().collection("friendRequests").document(requestId);
        DocumentReference friendshipRef = firebase.getFirestore().collection("friendships").document(requestId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(requestRef);
            if (!request.exists() || !"pending".equals(request.getString("status"))
                    || !currentUserId.equals(request.getString("receiverId"))) {
                throw new IllegalStateException("Zahtev vise nije aktivan.");
            }
            if (accept) {
                String senderId = request.getString("senderId");
                List<String> participants = sortedPair(senderId, currentUserId);
                Map<String, Object> friendship = new HashMap<>();
                friendship.put("user1Id", participants.get(0));
                friendship.put("user2Id", participants.get(1));
                friendship.put("participants", participants);
                friendship.put("createdAt", FieldValue.serverTimestamp());
                transaction.set(friendshipRef, friendship);
            }
            transaction.update(requestRef, "status", accept ? "accepted" : "rejected",
                    "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void updateRequestStatus(String requestId, String status,
                                     GameActionCallback callback) {
        DocumentReference reference = firebase.getFirestore().collection("friendRequests").document(requestId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot request = transaction.get(reference);
            if (!request.exists() || !"pending".equals(request.getString("status"))) {
                throw new IllegalStateException("Zahtev vise nije aktivan.");
            }
            if (!currentUserId.equals(request.getString("senderId"))
                    && !currentUserId.equals(request.getString("receiverId"))) {
                throw new IllegalStateException("Nemate pristup zahtevu.");
            }
            transaction.update(reference, "status", status,
                    "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void closeInvite(String inviteId, String status, GameActionCallback callback) {
        DocumentReference inviteRef = firebase.getFirestore().collection("matchInvites").document(inviteId);
        DocumentReference matchRef = firebase.getFirestore().collection("matches").document(inviteId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot invite = transaction.get(inviteRef);
            DocumentSnapshot match = transaction.get(matchRef);
            if (!invite.exists() || !"pending".equals(invite.getString("status"))) {
                throw new IllegalStateException("Poziv vise nije aktivan.");
            }
            if (!currentUserId.equals(invite.getString("senderId"))
                    && !currentUserId.equals(invite.getString("receiverId"))) {
                throw new IllegalStateException("Nemate pristup pozivu.");
            }
            if (!match.exists() || !"waiting".equals(match.getString("status"))
                    || match.getString("player2Id") != null) {
                throw new IllegalStateException("Poziv vise nije aktivan.");
            }
            transaction.update(inviteRef, "status", status,
                    "updatedAt", FieldValue.serverTimestamp());
            Map<String, Object> values = new HashMap<>();
            values.put("status", "abandoned");
            values.put("winnerId", null);
            values.put("loserId", null);
            values.put("abandonedByUserId", null);
            values.put("player1StarDelta", 0);
            values.put("player2StarDelta", 0);
            values.put("player1TokenReward", 0);
            values.put("player2TokenReward", 0);
            values.put("player1InGame", false);
            values.put("player2InGame", false);
            values.put("settlementApplied", true);
            values.put("currentGame", "none");
            values.put("currentTurnPlayerId", null);
            values.put("finishedAt", FieldValue.serverTimestamp());
            values.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(matchRef, values);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void expireInvite(String inviteId) {
        closeInvite(inviteId, "expired", new GameActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { }
        });
    }

    private boolean isPendingReceiverInvite(DocumentSnapshot invite) {
        return invite.exists() && "pending".equals(invite.getString("status"))
                && currentUserId.equals(invite.getString("receiverId"));
    }

    private void cancelWaitingMatch(String matchId) {
        if (matchId == null) return;
        completionRepository.abandonMatch(matchId, new GameActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { }
        });
    }

    private List<MatchInvite> allInvites() {
        List<MatchInvite> values = new ArrayList<>(incomingInvites);
        values.addAll(outgoingInvites);
        return values;
    }

    private FriendProfile profile(DocumentSnapshot document) {
        FriendProfile profile = new FriendProfile();
        profile.uid = document.getId();
        profile.username = text(document, "username");
        profile.avatarId = textOr(document, "avatarId", "M1");
        profile.avatarFrame = textOr(document, "avatarFrame", "standard");
        profile.stars = number(document, "stars");
        profile.monthlyStars = number(document, "monthlyStars");
        profile.league = number(document, "league");
        profile.isOnline = Boolean.TRUE.equals(document.getBoolean("isOnline"));
        profile.inGame = Boolean.TRUE.equals(document.getBoolean("inGame"));
        return profile;
    }

    private void emit() {
        if (listener == null) return;
        List<FriendProfile> friends = new ArrayList<>(friendProfiles.values());
        friends.sort(Comparator.comparing(profile -> profile.username.toLowerCase()));
        listener.onChanged(new FriendsSnapshot(friends, incomingRequests, outgoingRequests,
                incomingInvites, outgoingInvites));
    }

    private void report(Exception error) {
        if (listener != null) listener.onError(message(error));
    }

    private String pairId(String first, String second) {
        List<String> values = sortedPair(first, second);
        return values.get(0) + "_" + values.get(1);
    }

    private List<String> sortedPair(String first, String second) {
        List<String> values = new ArrayList<>(Arrays.asList(first, second));
        Collections.sort(values);
        return values;
    }

    private long number(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value;
    }

    private String text(DocumentSnapshot document, String field) {
        return textOr(document, field, "Igrac");
    }

    private String textOr(DocumentSnapshot document, String field, String fallback) {
        String value = document.getString(field);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String message(Exception error) {
        String value = error.getLocalizedMessage();
        return value == null ? "Operacija sa prijateljima nije uspela." : value;
    }
}
