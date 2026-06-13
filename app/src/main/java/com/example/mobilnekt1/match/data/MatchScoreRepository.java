package com.example.mobilnekt1.match.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;

public final class MatchScoreRepository {
    public static final int REQUIRED_GAMES = 4;

    private final FirebaseProvider firebase;

    public MatchScoreRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
    }

    public void commitGameResult(String matchId, String gameId, String gameStatusField,
                                 GameActionCallback callback) {
        FirebaseUser user = firebase.getCurrentUser();
        if (user == null) {
            callback.onError("Sesija je istekla.");
            return;
        }
        DocumentReference matchRef = firebase.getFirestore().collection("matches").document(matchId);
        DocumentReference gameRef = matchRef.collection("games").document(gameId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            DocumentSnapshot game = transaction.get(gameRef);
            if (!match.exists() || !game.exists()) {
                throw new IllegalStateException("Partija ili igra ne postoji.");
            }
            String player1Id = match.getString("player1Id");
            String player2Id = match.getString("player2Id");
            if (!user.getUid().equals(player1Id) && !user.getUid().equals(player2Id)) {
                throw new IllegalStateException("Niste ucesnik ove partije.");
            }
            if (!"finished".equals(game.getString(gameStatusField))) {
                throw new IllegalStateException("Igra jos nije zavrsena.");
            }

            List<String> completedGames = stringList(match.get("completedGames"));
            if (completedGames.contains(gameId)) {
                return null;
            }

            long player1Total = value(match.getLong("player1Score"))
                    + value(game.getLong("player1Score"));
            long player2Total = value(match.getLong("player2Score"))
                    + value(game.getLong("player2Score"));
            completedGames.add(gameId);

            transaction.update(matchRef,
                    "player1Score", player1Total,
                    "player2Score", player2Total,
                    "completedGames", completedGames,
                    "currentGame", "none",
                    "updatedAt", FieldValue.serverTimestamp());

            if (completedGames.size() >= REQUIRED_GAMES) {
                String winnerId = player1Total == player2Total ? null
                        : player1Total > player2Total ? player1Id : player2Id;
                transaction.update(matchRef,
                        "status", "finished",
                        "winnerId", winnerId,
                        "currentTurnPlayerId", null,
                        "updatedAt", FieldValue.serverTimestamp());
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<Object>) value) result.add(String.valueOf(item));
        }
        return result;
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private String message(Exception error) {
        return error.getLocalizedMessage() == null
                ? "Rezultat igre nije moguce sacuvati." : error.getLocalizedMessage();
    }
}
