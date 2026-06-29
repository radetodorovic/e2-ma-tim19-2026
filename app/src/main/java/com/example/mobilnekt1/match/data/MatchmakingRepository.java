package com.example.mobilnekt1.match.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public final class MatchmakingRepository {
    private static final int MAX_CLAIM_ATTEMPTS = 3;

    private final FirebaseProvider firebase;
    private final MatchRepository matchRepository;
    private final MatchCompletionRepository completionRepository;

    public MatchmakingRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
        matchRepository = new MatchRepository(context);
        completionRepository = new MatchCompletionRepository(context);
    }

    public void findRegularMatch(MatchCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla.");
            return;
        }
        matchRepository.createMatch(new MatchCallback() {
            @Override public void onSuccess(String ownMatchId) {
                claimQueue(user.getUid(), ownMatchId, 0, callback);
            }

            @Override public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void removeWaitingMatch(String matchId) {
        if (!firebase.isConfigured()) return;
        DocumentReference queueRef = queueReference();
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot queue = transaction.get(queueRef);
            if (queue.exists() && matchId.equals(queue.getString("matchId"))) {
                transaction.delete(queueRef);
            }
            return null;
        });
    }

    private void claimQueue(String userId, String ownMatchId, int attempt,
                            MatchCallback callback) {
        DocumentReference queueRef = queueReference();
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot queue = transaction.get(queueRef);
            String waitingUserId = queue.getString("waitingUserId");
            String waitingMatchId = queue.getString("matchId");
            if (!queue.exists() || waitingMatchId == null) {
                transaction.set(queueRef, queueValues(userId, ownMatchId));
                return QueueClaim.waiting(ownMatchId);
            }
            if (userId.equals(waitingUserId)) {
                return QueueClaim.waiting(waitingMatchId);
            }
            transaction.delete(queueRef);
            return QueueClaim.join(waitingMatchId);
        }).addOnSuccessListener(claim -> {
            if (!ownMatchId.equals(claim.matchId) && !claim.shouldJoin) {
                cancelOrphan(ownMatchId);
            }
            if (!claim.shouldJoin) {
                callback.onSuccess(claim.matchId);
                return;
            }
            matchRepository.joinMatch(claim.matchId, new MatchCallback() {
                @Override public void onSuccess(String joinedMatchId) {
                    cancelOrphan(ownMatchId);
                    callback.onSuccess(joinedMatchId);
                }

                @Override public void onError(String message) {
                    if (attempt + 1 >= MAX_CLAIM_ATTEMPTS) {
                        restoreWaiting(userId, ownMatchId, callback);
                    } else {
                        claimQueue(userId, ownMatchId, attempt + 1, callback);
                    }
                }
            });
        }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void restoreWaiting(String userId, String ownMatchId, MatchCallback callback) {
        DocumentReference queueRef = queueReference();
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot queue = transaction.get(queueRef);
            if (queue.exists()) return false;
            transaction.set(queueRef, queueValues(userId, ownMatchId));
            return true;
        }).addOnSuccessListener(restored -> {
            if (restored) {
                callback.onSuccess(ownMatchId);
            } else {
                cancelOrphan(ownMatchId);
                callback.onError("Uparivanje je promenjeno. Pokusajte ponovo.");
            }
        }).addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void cancelOrphan(String matchId) {
        completionRepository.abandonMatch(matchId, new com.example.mobilnekt1.games.shared.GameActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { }
        });
    }

    private Map<String, Object> queueValues(String userId, String matchId) {
        Map<String, Object> values = new HashMap<>();
        values.put("waitingUserId", userId);
        values.put("matchId", matchId);
        values.put("updatedAt", FieldValue.serverTimestamp());
        return values;
    }

    private DocumentReference queueReference() {
        return firebase.getFirestore().collection("matchmaking").document("regular");
    }

    private String message(Exception error) {
        String message = error.getLocalizedMessage();
        return message == null ? "Nasumicno uparivanje nije uspelo." : message;
    }

    private static final class QueueClaim {
        final String matchId;
        final boolean shouldJoin;

        private QueueClaim(String matchId, boolean shouldJoin) {
            this.matchId = matchId;
            this.shouldJoin = shouldJoin;
        }

        static QueueClaim waiting(String matchId) {
            return new QueueClaim(matchId, false);
        }

        static QueueClaim join(String matchId) {
            return new QueueClaim(matchId, true);
        }
    }
}
