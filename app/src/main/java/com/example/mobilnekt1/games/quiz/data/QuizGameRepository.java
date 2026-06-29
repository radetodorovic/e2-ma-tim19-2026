package com.example.mobilnekt1.games.quiz.data;

import android.content.Context;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.content.GameContentRepository;
import com.example.mobilnekt1.games.quiz.domain.QuizQuestion;
import com.example.mobilnekt1.games.quiz.domain.QuizScoringEngine;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuizGameRepository {
    public static final int QUESTION_SECONDS = 5;
    private final FirebaseProvider firebase;
    private final GameContentRepository contentRepository;
    private ListenerRegistration registration;

    public QuizGameRepository(Context context) {
        firebase = FirebaseProvider.getInstance(context);
        contentRepository = new GameContentRepository(firebase.getFirestore());
    }

    public String currentUserId() {
        FirebaseUser user = firebase.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void initialize(String matchId, GameActionCallback callback) {
        contentRepository.loadQuizQuestions(new GameContentRepository.Callback<List<QuizQuestion>>() {
            @Override public void onSuccess(List<QuizQuestion> questions) {
                initializeWithQuestions(matchId, questions, callback);
            }
            @Override public void onError(String message) { callback.onError(message); }
        });
    }

    private void initializeWithQuestions(String matchId, List<QuizQuestion> questions,
                                         GameActionCallback callback) {
        DocumentReference matchRef = matchRef(matchId);
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot match = transaction.get(matchRef);
            requireParticipant(match);
            if (!transaction.get(gameRef).exists()) {
                String abandoned = match.getString("abandonedByUserId");
                String player1Id = match.getString("player1Id");
                String player2Id = match.getString("player2Id");
                Map<String, Object> data = new HashMap<>();
                data.put("player1Id", player1Id);
                data.put("player2Id", player2Id);
                data.put("abandonedPlayerId", abandoned);
                data.put("status", "active");
                data.put("currentQuestionIndex", 0);
                data.put("deadlineMillis", System.currentTimeMillis() + QUESTION_SECONDS * 1000L);
                data.put("questions", questionMaps(questions));
                data.put("player1Answer", null);
                data.put("player2Answer", null);
                data.put("player1AnsweredAt", null);
                data.put("player2AnsweredAt", null);
                data.put("player1Submitted", player1Id.equals(abandoned));
                data.put("player2Submitted", player2Id.equals(abandoned));
                data.put("player1Score", 0);
                data.put("player2Score", 0);
                data.put("player1Correct", 0);
                data.put("player1Wrong", 0);
                data.put("player2Correct", 0);
                data.put("player2Wrong", 0);
                data.put("updatedAt", FieldValue.serverTimestamp());
                transaction.set(gameRef, data);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void listen(String matchId, QuizGameListener listener) {
        stopListening();
        registration = gameRef(matchId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(message(error));
            } else if (snapshot != null && snapshot.exists()) {
                com.example.mobilnekt1.games.quiz.domain.QuizGameState state =
                        snapshot.toObject(com.example.mobilnekt1.games.quiz.domain.QuizGameState.class);
                if (state != null) listener.onChanged(state);
            }
        });
    }

    public void submitAnswer(String matchId, int answerIndex, GameActionCallback callback) {
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot game = transaction.get(gameRef);
            requireGameParticipant(game);
            if (!"active".equals(game.getString("status"))) {
                throw new IllegalStateException("Kviz je zavrsen.");
            }
            Long deadline = game.getLong("deadlineMillis");
            if (deadline == null || deadline < System.currentTimeMillis()) {
                throw new IllegalStateException("Vreme za odgovor je isteklo.");
            }
            boolean player1 = currentUserId().equals(game.getString("player1Id"));
            String submittedField = player1 ? "player1Submitted" : "player2Submitted";
            if (Boolean.TRUE.equals(game.getBoolean(submittedField))) {
                throw new IllegalStateException("Odgovor je vec predat.");
            }
            transaction.update(gameRef,
                    player1 ? "player1Answer" : "player2Answer", answerIndex,
                    player1 ? "player1AnsweredAt" : "player2AnsweredAt", System.currentTimeMillis(),
                    submittedField, true,
                    "updatedAt", FieldValue.serverTimestamp());
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void resolveQuestion(String matchId, GameActionCallback callback) {
        DocumentReference gameRef = gameRef(matchId);
        firebase.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot game = transaction.get(gameRef);
            requireGameParticipant(game);
            if (!"active".equals(game.getString("status"))) return null;
            Long deadline = game.getLong("deadlineMillis");
            String abandoned = game.getString("abandonedPlayerId");
            boolean player1Submitted = Boolean.TRUE.equals(game.getBoolean("player1Submitted"))
                    || game.getString("player1Id").equals(abandoned);
            boolean player2Submitted = Boolean.TRUE.equals(game.getBoolean("player2Submitted"))
                    || game.getString("player2Id").equals(abandoned);
            boolean bothSubmitted = player1Submitted && player2Submitted;
            if (!bothSubmitted && (deadline == null || deadline > System.currentTimeMillis())) return null;

            int questionIndex = intValue(game.getLong("currentQuestionIndex"));
            List<QuizQuestion> questions = readQuestions(game);
            if (questionIndex >= questions.size()) return null;
            QuizQuestion question = questions.get(questionIndex);
            Integer p1Answer = nullableInt(game.getLong("player1Answer"));
            Integer p2Answer = nullableInt(game.getLong("player2Answer"));
            Long p1Time = game.getLong("player1AnsweredAt");
            Long p2Time = game.getLong("player2AnsweredAt");
            QuizScoringEngine.Result scored = QuizScoringEngine.score(
                    p1Answer, p1Time, p2Answer, p2Time, question.correctIndex);
            boolean p1Correct = scored.player1Correct;
            boolean p2Correct = scored.player2Correct;
            int p1Points = scored.player1Points;
            int p2Points = scored.player2Points;
            Map<String, Object> updates = new HashMap<>();
            updates.put("player1Score", FieldValue.increment(p1Points));
            updates.put("player2Score", FieldValue.increment(p2Points));
            if (p1Answer != null) updates.put(p1Correct ? "player1Correct" : "player1Wrong", FieldValue.increment(1));
            if (p2Answer != null) updates.put(p2Correct ? "player2Correct" : "player2Wrong", FieldValue.increment(1));
            if (questionIndex >= questions.size() - 1) {
                updates.put("status", "finished");
                updates.put("deadlineMillis", 0);
            } else {
                updates.put("currentQuestionIndex", questionIndex + 1);
                updates.put("deadlineMillis", System.currentTimeMillis() + QUESTION_SECONDS * 1000L);
                updates.put("player1Answer", null);
                updates.put("player2Answer", null);
                updates.put("player1AnsweredAt", null);
                updates.put("player2AnsweredAt", null);
                updates.put("player1Submitted", game.getString("player1Id").equals(abandoned));
                updates.put("player2Submitted", game.getString("player2Id").equals(abandoned));
            }
            updates.put("updatedAt", FieldValue.serverTimestamp());
            transaction.update(gameRef, updates);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private List<Map<String, Object>> questionMaps(List<QuizQuestion> questions) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (QuizQuestion question : questions) {
            Map<String, Object> map = new HashMap<>();
            map.put("text", question.text);
            map.put("answers", question.answers);
            map.put("correctIndex", question.correctIndex);
            result.add(map);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<QuizQuestion> readQuestions(DocumentSnapshot snapshot) {
        List<QuizQuestion> result = new ArrayList<>();
        Object value = snapshot.get("questions");
        if (!(value instanceof List)) return result;
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) item;
            List<String> answers = new ArrayList<>();
            Object answerValue = map.get("answers");
            if (answerValue instanceof List) {
                for (Object answer : (List<?>) answerValue) answers.add(String.valueOf(answer));
            }
            Number correct = (Number) map.get("correctIndex");
            result.add(new QuizQuestion(String.valueOf(map.get("text")), answers,
                    correct == null ? 0 : correct.intValue()));
        }
        return result;
    }

    private void requireParticipant(DocumentSnapshot match) {
        String uid = currentUserId();
        if (!match.exists() || uid == null || (!uid.equals(match.getString("player1Id"))
                && !uid.equals(match.getString("player2Id")))) {
            throw new IllegalStateException("Niste ucesnik ove partije.");
        }
    }

    private void requireGameParticipant(DocumentSnapshot game) {
        String uid = currentUserId();
        if (!game.exists() || uid == null || (!uid.equals(game.getString("player1Id"))
                && !uid.equals(game.getString("player2Id")))) {
            throw new IllegalStateException("Niste ucesnik ove partije.");
        }
    }

    private int intValue(Long value) { return value == null ? 0 : value.intValue(); }
    private Integer nullableInt(Long value) { return value == null ? null : value.intValue(); }
    private DocumentReference matchRef(String matchId) { return firebase.getFirestore().collection("matches").document(matchId); }
    private DocumentReference gameRef(String matchId) { return matchRef(matchId).collection("games").document("koZnaZna"); }
    public void stopListening() { if (registration != null) { registration.remove(); registration = null; } }
    private String message(Exception error) { return error.getLocalizedMessage() == null ? "Sinhronizacija kviza nije uspela." : error.getLocalizedMessage(); }
}
