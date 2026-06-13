package com.example.mobilnekt1.games.content;

import com.example.mobilnekt1.games.quiz.domain.QuizQuestion;
import com.example.mobilnekt1.games.stepbystep.StepPuzzle;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GameContentRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    public static final class ConnectionsPuzzle {
        public final List<String> leftItems;
        public final List<String> rightItems;
        public final List<Integer> correctMatches;

        public ConnectionsPuzzle(List<String> leftItems, List<String> rightItems,
                                 List<Integer> correctMatches) {
            this.leftItems = leftItems;
            this.rightItems = rightItems;
            this.correctMatches = correctMatches;
        }
    }

    private final FirebaseFirestore firestore;

    public GameContentRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public void loadQuizQuestions(Callback<List<QuizQuestion>> callback) {
        firestore.collection("quizQuestions").orderBy("order").limit(5).get()
                .addOnSuccessListener(snapshot -> {
                    List<QuizQuestion> loaded = quizFrom(snapshot);
                    if (loaded.size() >= 5) callback.onSuccess(loaded);
                    else seedQuiz(callback);
                })
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void loadConnectionsPuzzles(Callback<List<ConnectionsPuzzle>> callback) {
        firestore.collection("connectionsPuzzles").orderBy("order").limit(2).get()
                .addOnSuccessListener(snapshot -> {
                    List<ConnectionsPuzzle> loaded = connectionsFrom(snapshot);
                    if (loaded.size() >= 2) callback.onSuccess(loaded);
                    else seedConnections(callback);
                })
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    public void loadStepPuzzles(Callback<List<StepPuzzle>> callback) {
        firestore.collection("stepPuzzles").orderBy("order").limit(2).get()
                .addOnSuccessListener(snapshot -> {
                    List<StepPuzzle> loaded = stepsFrom(snapshot);
                    if (loaded.size() >= 2) callback.onSuccess(loaded);
                    else seedSteps(callback);
                })
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void seedQuiz(Callback<List<QuizQuestion>> callback) {
        List<QuizQuestion> defaults = defaultQuiz();
        WriteBatch batch = firestore.batch();
        for (int i = 0; i < defaults.size(); i++) {
            QuizQuestion question = defaults.get(i);
            Map<String, Object> data = new HashMap<>();
            data.put("order", i);
            data.put("text", question.text);
            data.put("answers", question.answers);
            data.put("correctIndex", question.correctIndex);
            batch.set(firestore.collection("quizQuestions").document("question" + (i + 1)), data);
        }
        batch.commit().addOnSuccessListener(unused -> callback.onSuccess(defaults))
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void seedConnections(Callback<List<ConnectionsPuzzle>> callback) {
        List<ConnectionsPuzzle> defaults = defaultConnections();
        WriteBatch batch = firestore.batch();
        for (int i = 0; i < defaults.size(); i++) {
            ConnectionsPuzzle puzzle = defaults.get(i);
            Map<String, Object> data = new HashMap<>();
            data.put("order", i);
            data.put("leftItems", puzzle.leftItems);
            data.put("rightItems", puzzle.rightItems);
            data.put("correctMatches", puzzle.correctMatches);
            batch.set(firestore.collection("connectionsPuzzles").document("round" + (i + 1)), data);
        }
        batch.commit().addOnSuccessListener(unused -> callback.onSuccess(defaults))
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private void seedSteps(Callback<List<StepPuzzle>> callback) {
        List<StepPuzzle> defaults = defaultSteps();
        WriteBatch batch = firestore.batch();
        for (int i = 0; i < defaults.size(); i++) {
            StepPuzzle puzzle = defaults.get(i);
            Map<String, Object> data = new HashMap<>();
            data.put("order", i);
            data.put("solution", puzzle.solution);
            data.put("hints", Arrays.asList(puzzle.hints));
            batch.set(firestore.collection("stepPuzzles").document("puzzle" + (i + 1)), data);
        }
        batch.commit().addOnSuccessListener(unused -> callback.onSuccess(defaults))
                .addOnFailureListener(error -> callback.onError(message(error)));
    }

    private List<QuizQuestion> quizFrom(QuerySnapshot snapshot) {
        List<QuizQuestion> result = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            QuizQuestion question = document.toObject(QuizQuestion.class);
            if (question != null && question.answers != null && question.answers.size() == 4) {
                result.add(question);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ConnectionsPuzzle> connectionsFrom(QuerySnapshot snapshot) {
        List<ConnectionsPuzzle> result = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            List<String> left = (List<String>) document.get("leftItems");
            List<String> right = (List<String>) document.get("rightItems");
            List<Long> matches = (List<Long>) document.get("correctMatches");
            if (left == null || right == null || matches == null || left.size() != 5
                    || right.size() != 5 || matches.size() != 5) continue;
            List<Integer> converted = new ArrayList<>();
            for (Long match : matches) converted.add(match.intValue());
            result.add(new ConnectionsPuzzle(left, right, converted));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<StepPuzzle> stepsFrom(QuerySnapshot snapshot) {
        List<StepPuzzle> result = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            String solution = document.getString("solution");
            List<String> hints = (List<String>) document.get("hints");
            if (solution != null && hints != null && hints.size() == 7) {
                result.add(new StepPuzzle(solution, hints.toArray(new String[0])));
            }
        }
        return result;
    }

    private List<QuizQuestion> defaultQuiz() {
        return Arrays.asList(
                new QuizQuestion("Koji je glavni grad Srbije?", Arrays.asList("Novi Sad", "Beograd", "Nis", "Kragujevac"), 1),
                new QuizQuestion("Koja planeta je najbliza Suncu?", Arrays.asList("Venera", "Mars", "Merkur", "Zemlja"), 2),
                new QuizQuestion("Koliko kontinenata postoji?", Arrays.asList("Pet", "Sest", "Sedam", "Osam"), 2),
                new QuizQuestion("Ko je napisao Na Drini cuprija?", Arrays.asList("Mesa Selimovic", "Ivo Andric", "Branko Copic", "Milos Crnjanski"), 1),
                new QuizQuestion("Koji hemijski simbol predstavlja zlato?", Arrays.asList("Ag", "Fe", "Au", "Cu"), 2));
    }

    private List<ConnectionsPuzzle> defaultConnections() {
        return Arrays.asList(
                new ConnectionsPuzzle(Arrays.asList("Nikola Tesla", "Mihajlo Pupin", "Ivo Andric", "Novak Djokovic", "Marina Abramovic"), Arrays.asList("Tenis", "Performans", "Na Drini cuprija", "Naizmenicna struja", "Kalemovi"), Arrays.asList(3, 4, 2, 0, 1)),
                new ConnectionsPuzzle(Arrays.asList("Francuska", "Italija", "Spanija", "Grcka", "Austrija"), Arrays.asList("Bec", "Atina", "Madrid", "Rim", "Pariz"), Arrays.asList(4, 3, 2, 1, 0)));
    }

    private List<StepPuzzle> defaultSteps() {
        return Arrays.asList(
                new StepPuzzle("Android", new String[]{"Koristi se svakodnevno na velikom broju uredjaja.", "Ima verzije sa imenima i brojevima.", "Za njega se aplikacije prave u posebnom razvojnom okruzenju.", "Cesto se koristi Java ili Kotlin.", "Ima aktivnosti, fragmente i XML rasporede.", "Pokrece se na mobilnim telefonima i tabletima.", "Operativni sistem iz Google ekosistema."}),
                new StepPuzzle("Dunav", new String[]{"Povezuje veliki broj gradova i kultura.", "Njegov tok prolazi kroz vise evropskih drzava.", "Vazan je za saobracaj i trgovinu.", "Prolazi kroz Bec, Budimpestu i Beograd.", "U Srbiju ulazi kod Batine.", "Jedna je od najduzih evropskih reka.", "Uliva se u Crno more."}));
    }

    private String message(Exception error) {
        return error.getLocalizedMessage() == null ? "Sadrzaj igre nije moguce ucitati."
                : error.getLocalizedMessage();
    }
}
