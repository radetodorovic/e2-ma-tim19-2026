package com.example.mobilnekt1.games.associations.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.associations.domain.AssociationGameState;
import com.example.mobilnekt1.games.associations.domain.AssociationScoringEngine;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssociationGameRepository {
    private static final long ROUND_MILLIS = 120_000L;
    private static final long ROUND_BREAK_MILLIS = 4_000L;
    private final FirebaseProvider firebase;
    private ListenerRegistration registration;

    private static final String[][][] PUZZLE_FIELDS = {
            {{"Kos", "Tabla", "Lopta", "Parket"}, {"Servis", "Mreza", "Reket", "Set"},
                    {"Gol", "Kopacke", "Penal", "Stadion"}, {"Bazen", "Kapica", "Staza", "Plivanje"}},
            {{"Java", "Kotlin", "Python", "C"}, {"Mis", "Tastatura", "Monitor", "Procesor"},
                    {"Android", "Linux", "Windows", "iOS"}, {"Internet", "Mreza", "Server", "Klijent"}}
    };
    private static final String[][] COLUMN_SOLUTIONS = {
            {"Kosarka", "Tenis", "Fudbal", "Plivanje"},
            {"Programiranje", "Racunar", "Operativni sistem", "Komunikacija"}
    };
    private static final String[] FINAL_SOLUTIONS = {"Sport", "Tehnologija"};

    public AssociationGameRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
    }

    public String currentUserId() {
        FirebaseUser user = firebase.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void initialize(String matchId, GameActionCallback callback) {
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            if (!transaction.get(gameRef).exists()) {
                transaction.set(gameRef, roundData(match, 0, match.getString("player1Id"), 0, 0, 0, 0));
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void listen(String matchId, AssociationGameListener listener) {
        stopListening();
        registration = gameRef(matchId).addSnapshotListener((snapshot, error) -> {
            if (error != null) listener.onError(message(error));
            else if (snapshot != null && snapshot.exists()) {
                AssociationGameState state = snapshot.toObject(AssociationGameState.class);
                if (state != null) listener.onChanged(state);
            }
        });
    }

    public void openField(String matchId, int index, GameActionCallback callback) {
        update(matchId, callback, (transaction, match, game, gameRef) -> {
            requireTurn(game);
            requireTurnStage(game, "open", "Vec ste otvorili polje. Sada pokusajte resenje.");
            if (index < 0 || index >= 16) throw new IllegalStateException("Polje nije ispravno.");
            List<Long> opened = longList(game.get("openedFields"));
            int column = index / 4;
            if (opened.contains((long) index) || longList(game.get("solvedColumns")).contains((long) column)) return;
            opened.add((long) index);
            transaction.update(gameRef, "openedFields", opened, "turnStage", "guess",
                    "updatedAt", FieldValue.serverTimestamp());
        });
    }

    public void passTurn(String matchId, GameActionCallback callback) {
        update(matchId, callback, (transaction, match, game, gameRef) -> {
            requireTurn(game);
            switchTurn(transaction, match, gameRef);
        });
    }

    public void submitColumn(String matchId, int column, String answer, GameActionCallback callback) {
        update(matchId, callback, (transaction, match, game, gameRef) -> {
            requireTurn(game);
            requireTurnStage(game, "guess", "Prvo otvorite jedno polje.");
            List<Long> solved = longList(game.get("solvedColumns"));
            if (column < 0 || column >= 4 || solved.contains((long) column)) return;
            List<String> solutions = stringList(game.get("columnSolutions"));
            if (!same(answer, solutions.get(column))) {
                switchTurn(transaction, match, gameRef);
                return;
            }
            List<Long> opened = longList(game.get("openedFields"));
            int openedInColumn = 0;
            for (int row = 0; row < 4; row++) {
                long index = column * 4L + row;
                if (opened.contains(index)) openedInColumn++;
                else opened.add(index);
            }
            solved.add((long) column);
            int points = AssociationScoringEngine.columnPoints(openedInColumn);
            transaction.update(gameRef, "openedFields", opened, "solvedColumns", solved,
                    scoreField(match), FieldValue.increment(points),
                    "turnStage", "finalOnly",
                    "updatedAt", FieldValue.serverTimestamp());
        });
    }

    public void submitFinal(String matchId, String answer, GameActionCallback callback) {
        update(matchId, callback, (transaction, match, game, gameRef) -> {
            requireTurn(game);
            String turnStage = turnStage(game);
            if (!"guess".equals(turnStage) && !"finalOnly".equals(turnStage)) {
                throw new IllegalStateException("Prvo otvorite jedno polje.");
            }
            if (!same(answer, game.getString("finalSolution"))) {
                switchTurn(transaction, match, gameRef);
                return;
            }
            List<Long> opened = longList(game.get("openedFields"));
            List<Long> solved = longList(game.get("solvedColumns"));
            int[] openedCounts = new int[4];
            boolean[] solvedColumns = new boolean[4];
            for (Long index : opened) openedCounts[index.intValue() / 4]++;
            for (Long column : solved) solvedColumns[column.intValue()] = true;
            int points = AssociationScoringEngine.finalPoints(openedCounts, solvedColumns);
            finishRound(transaction, match, game, gameRef, currentUserId(), points);
        });
    }

    public void advanceExpired(String matchId, GameActionCallback callback) {
        update(matchId, callback, (transaction, match, game, gameRef) -> {
            Long deadline = game.getLong("deadlineMillis");
            if (deadline != null && deadline <= System.currentTimeMillis()) {
                if ("roundBreak".equals(game.getString("phase"))) {
                    transaction.set(gameRef, roundData(match, 1, match.getString("player2Id"),
                            value(game.getLong("player1Score")),
                            value(game.getLong("player2Score")),
                            value(game.getLong("player1SolvedRounds")),
                            value(game.getLong("player2SolvedRounds"))));
                } else {
                    finishRound(transaction, match, game, gameRef, null, 0);
                }
            }
        });
    }

    private void finishRound(Transaction transaction, DocumentSnapshot match, DocumentSnapshot game,
                             DocumentReference gameRef, String scoringPlayer, int points) {
        long p1 = value(game.getLong("player1Score"));
        long p2 = value(game.getLong("player2Score"));
        long p1Solved = value(game.getLong("player1SolvedRounds"));
        long p2Solved = value(game.getLong("player2SolvedRounds"));
        if (scoringPlayer != null) {
            if (scoringPlayer.equals(match.getString("player1Id"))) { p1 += points; p1Solved++; }
            else { p2 += points; p2Solved++; }
        }
        int round = (int) value(game.getLong("round"));
        if (round >= 1) {
            transaction.update(gameRef, "player1Score", p1, "player2Score", p2,
                    "phase", "finished", "deadlineMillis", 0,
                    "player1SolvedRounds", p1Solved, "player2SolvedRounds", p2Solved,
                    "updatedAt", FieldValue.serverTimestamp());
        } else {
            transaction.update(gameRef,
                    "player1Score", p1,
                    "player2Score", p2,
                    "player1SolvedRounds", p1Solved,
                    "player2SolvedRounds", p2Solved,
                    "phase", "roundBreak",
                    "activePlayerId", null,
                    "turnStage", "open",
                    "deadlineMillis", System.currentTimeMillis() + ROUND_BREAK_MILLIS,
                    "updatedAt", FieldValue.serverTimestamp());
        }
    }

    private Map<String, Object> roundData(DocumentSnapshot match, int round, String starter,
                                          long player1Score, long player2Score,
                                          long player1Solved, long player2Solved) {
        Map<String, Object> data = new HashMap<>();
        data.put("player1Id", match.getString("player1Id"));
        data.put("player2Id", match.getString("player2Id"));
        data.put("round", round);
        data.put("phase", "playing");
        data.put("startingPlayerId", starter);
        String abandoned = match.getString("abandonedByUserId");
        String activePlayer = starter.equals(abandoned) ? otherPlayer(match, starter) : starter;
        data.put("abandonedPlayerId", abandoned);
        data.put("activePlayerId", activePlayer);
        data.put("turnStage", "open");
        data.put("deadlineMillis", System.currentTimeMillis() + ROUND_MILLIS);
        List<String> fields = new ArrayList<>();
        for (String[] column : PUZZLE_FIELDS[round]) fields.addAll(Arrays.asList(column));
        data.put("fields", fields);
        data.put("columnSolutions", Arrays.asList(COLUMN_SOLUTIONS[round]));
        data.put("finalSolution", FINAL_SOLUTIONS[round]);
        data.put("openedFields", new ArrayList<>());
        data.put("solvedColumns", new ArrayList<>());
        data.put("player1Score", player1Score);
        data.put("player2Score", player2Score);
        data.put("player1SolvedRounds", player1Solved);
        data.put("player2SolvedRounds", player2Solved);
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private void update(String matchId, GameActionCallback callback, Operation operation) {
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            DocumentSnapshot game = transaction.get(gameRef);
            if (!game.exists() || "finished".equals(game.getString("phase"))) return null;
            operation.run(transaction, match, game, gameRef);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void requireTurn(DocumentSnapshot game) {
        if (!currentUserId().equals(game.getString("activePlayerId")))
            throw new IllegalStateException("Protivnik je na potezu.");
    }
    private void requireTurnStage(DocumentSnapshot game, String expected, String message) {
        if (!expected.equals(turnStage(game))) throw new IllegalStateException(message);
    }
    private String turnStage(DocumentSnapshot game) {
        String value = game.getString("turnStage");
        return value == null ? "open" : value;
    }
    private void switchTurn(Transaction transaction, DocumentSnapshot match,
                            DocumentReference gameRef) {
        String nextPlayer = otherPlayer(match, currentUserId());
        if (nextPlayer.equals(match.getString("abandonedByUserId"))) {
            nextPlayer = currentUserId();
        }
        transaction.update(gameRef,
                "activePlayerId", nextPlayer,
                "turnStage", "open",
                "updatedAt", FieldValue.serverTimestamp());
    }
    private String scoreField(DocumentSnapshot match) {
        return currentUserId().equals(match.getString("player1Id")) ? "player1Score" : "player2Score";
    }
    private String otherPlayer(DocumentSnapshot match, String uid) {
        return uid.equals(match.getString("player1Id")) ? match.getString("player2Id") : match.getString("player1Id");
    }
    private boolean same(String first, String second) { return normalize(first).equals(normalize(second)); }
    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    private void requireParticipant(DocumentSnapshot match) {
        String uid = currentUserId();
        if (!match.exists() || uid == null || (!uid.equals(match.getString("player1Id"))
                && !uid.equals(match.getString("player2Id"))))
            throw new IllegalStateException("Niste ucesnik ove partije.");
    }
    @SuppressWarnings("unchecked") private List<Long> longList(Object value) {
        List<Long> result = new ArrayList<>();
        if (value instanceof List) for (Object item : (List<Object>) value) result.add(((Number) item).longValue());
        return result;
    }
    @SuppressWarnings("unchecked") private List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) for (Object item : (List<Object>) value) result.add(String.valueOf(item));
        return result;
    }
    private long value(Long value) { return value == null ? 0 : value; }
    private DocumentReference matchRef(String matchId) { return firebase.getFirestore().collection("matches").document(matchId); }
    private DocumentReference gameRef(String matchId) { return matchRef(matchId).collection("games").document("associations"); }
    public void stopListening() { if (registration != null) { registration.remove(); registration = null; } }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Sinhronizacija Asocijacija nije uspela." : error.getLocalizedMessage(); }
    private interface Operation { void run(Transaction transaction, DocumentSnapshot match, DocumentSnapshot game, DocumentReference gameRef); }
}
