package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.mobilnekt1.challenges.data.ChallengeRepository;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.mobilnekt1.challenges.domain.Challenge;
import java.util.*;

public final class ChallengePlayActivity extends BaseKt1Activity {
    public static final String EXTRA_CHALLENGE_ID = "challengeId";
    private static final String[] GAMES = {"koZnaZna", "spojnice", "associations", "skocko", "stepByStep", "myNumber"};
    private static final Class<?>[] ACTIVITIES = {QuizActivity.class, ConnectionsActivity.class,
            AssociationsActivity.class, SkockoActivity.class, StepByStepActivity.class, MyNumberActivity.class};
    private String challengeId;
    private int completedCount;
    private long totalScore;
    private ListenerRegistration registration;
    private ListenerRegistration challengeRegistration;
    private ChallengeRepository repository;
    private TextView progress, total;
    private Button play;
    private TextView results;
    private Button claim;
    private boolean scorePublished;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                int score = result.getData().getIntExtra(EXTRA_CHALLENGE_SCORE, 0);
                repository.saveGameScore(challengeId, GAMES[completedCount], score, callback());
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_challenge_play);
        challengeId = getIntent().getStringExtra(EXTRA_CHALLENGE_ID);
        if (challengeId == null) { finish(); return; }
        repository = new ChallengeRepository(this); progress = findViewById(R.id.text_challenge_progress);
        total = findViewById(R.id.text_challenge_total); play = findViewById(R.id.button_play_challenge_game);
        results = findViewById(R.id.text_challenge_results); claim = findViewById(R.id.button_claim_challenge_reward);
        play.setOnClickListener(v -> launchNext()); listen();
        claim.setOnClickListener(v -> repository.claimReward(challengeId, callback()));
    }
    private void listen() {
        String uid = FirebaseProvider.getInstance(this).getCurrentUser().getUid();
        registration = FirebaseProvider.getInstance(this).getFirestore().collection("challenges")
                .document(challengeId).collection("runs").document(uid).addSnapshotListener((run, error) -> {
                    if (error != null) { showInfoDialog("Izazov", error.getLocalizedMessage()); return; }
                    completedCount = run != null && run.exists() && run.get("completedGames") instanceof java.util.List
                            ? ((java.util.List<?>) run.get("completedGames")).size() : 0;
                    Long score = run == null ? null : run.getLong("totalScore"); totalScore = score == null ? 0 : score;
                    render();
                });
        challengeRegistration = FirebaseProvider.getInstance(this).getFirestore().collection("challenges")
                .document(challengeId).addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null || !snapshot.exists() || error != null) return;
                    Challenge challenge = snapshot.toObject(Challenge.class);
                    if (challenge != null) renderResults(challenge, uid);
                });
    }
    private void render() {
        progress.setText("Zavrseno: " + completedCount + "/6" + (completedCount < 6 ? "\nSledece: " + GAMES[completedCount] : ""));
        total.setText("Ukupno bodova: " + totalScore); play.setEnabled(completedCount < 6);
        if (completedCount == 6) play.setText("Izazov zavrsen");
        if (completedCount == 6 && !scorePublished) {
            scorePublished = true;
            repository.publishFinishedScore(challengeId, totalScore, callback());
        }
    }
    private void renderResults(Challenge challenge, String uid) {
        int participantCount = challenge.participantIds == null ? 0 : challenge.participantIds.size();
        int finishedCount = challenge.participantScores == null ? 0 : challenge.participantScores.size();
        if (!"finished".equals(challenge.status)) {
            results.setText("Cekaju se ostali igraci: " + finishedCount + "/" + participantCount);
            claim.setVisibility(android.view.View.GONE); return;
        }
        List<Map.Entry<String, Long>> ranking = new ArrayList<>(challenge.participantScores.entrySet());
        Collections.sort(ranking, (a, b) -> Long.compare(b.getValue(), a.getValue()));
        StringBuilder text = new StringBuilder("Konacan plasman:\n");
        for (int i = 0; i < ranking.size(); i++) {
            String storedName = challenge.participantNames == null ? null
                    : challenge.participantNames.get(ranking.get(i).getKey());
            String name = ranking.get(i).getKey().equals(uid) ? "Vi (" + (storedName == null ? "igrac" : storedName) + ")"
                    : (storedName == null ? "Igrac " + ranking.get(i).getKey().substring(0, 6) : storedName);
            text.append(i + 1).append(". ").append(name).append(" - ").append(ranking.get(i).getValue()).append(" bodova\n");
        }
        results.setText(text.toString());
        boolean claimed = challenge.claimedIds != null && challenge.claimedIds.contains(uid);
        claim.setVisibility(claimed ? android.view.View.GONE : android.view.View.VISIBLE);
    }
    private void launchNext() {
        if (completedCount >= 6) return;
        Intent intent = new Intent(this, ACTIVITIES[completedCount]);
        intent.putExtra(EXTRA_CHALLENGE_ID, challengeId);
        intent.putExtra(EXTRA_CHALLENGE_GAME, true); launcher.launch(intent);
    }
    private GameActionCallback callback() { return new GameActionCallback() {
        @Override public void onSuccess() { }
        @Override public void onError(String message) { showInfoDialog("Izazov", message); }
    }; }
    @Override protected void onDestroy() {
        if (registration != null) registration.remove();
        if (challengeRegistration != null) challengeRegistration.remove();
        super.onDestroy();
    }
}
