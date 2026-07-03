package com.example.mobilnekt1;

import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import com.example.mobilnekt1.challenges.data.*;
import com.example.mobilnekt1.challenges.domain.Challenge;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import java.util.List;

public final class ChallengeActivity extends BaseKt1Activity {
    private ChallengeRepository repository;
    private LinearLayout container;
    private EditText starsInput, tokensInput;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_challenge);
        container = findViewById(R.id.container_challenges);
        starsInput = findViewById(R.id.input_challenge_stars);
        tokensInput = findViewById(R.id.input_challenge_tokens);
        repository = new ChallengeRepository(this);
        findViewById(R.id.button_create_challenge).setOnClickListener(v ->
                repository.create(number(starsInput), number(tokensInput), callback()));
        repository.listen(new ChallengeListener() {
            @Override public void onChanged(List<Challenge> values, String uid) { render(values, uid); }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.challenges_title), message); }
        });
    }

    private void render(List<Challenge> values, String uid) {
        container.removeAllViews();
        for (Challenge value : values) {
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 16, 16, 16); card.setBackgroundResource(R.drawable.card_background);
            TextView text = new TextView(this);
            text.setText(value.creatorName + "\nUlog: " + value.starStake + " zvezda, "
                    + value.tokenStake + " tokena\nUcesnici: " + value.participantIds.size() + "/4");
            card.addView(text);
            if ("open".equals(value.status) && !value.participantIds.contains(uid)) {
                Button join = new Button(this); join.setText(R.string.challenge_join);
                join.setOnClickListener(v -> repository.join(value.id, callback())); card.addView(join);
            }
            if ("open".equals(value.status) && uid.equals(value.creatorId)
                    && value.participantIds.size() == 1) {
                Button cancel = new Button(this); cancel.setText("Otkazi i vrati ulog");
                cancel.setOnClickListener(v -> repository.cancel(value.id, callback())); card.addView(cancel);
            }
            if ("open".equals(value.status) && uid.equals(value.creatorId)
                    && value.participantIds.size() >= 2) {
                Button start = new Button(this); start.setText(R.string.start);
                start.setOnClickListener(v -> repository.start(value.id, callback())); card.addView(start);
            }
            if (("active".equals(value.status) || "finished".equals(value.status))
                    && value.participantIds.contains(uid)) {
                Button play = new Button(this); play.setText("finished".equals(value.status)
                        ? "Pogledaj rezultat" : "Odigraj izazov");
                play.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ChallengePlayActivity.class);
                    intent.putExtra(ChallengePlayActivity.EXTRA_CHALLENGE_ID, value.id); startActivity(intent);
                });
                card.addView(play);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, 12); container.addView(card, params);
        }
        if (values.isEmpty()) { TextView empty = new TextView(this); empty.setText(R.string.no_items); container.addView(empty); }
    }
    private long number(EditText input) { try { return Long.parseLong(input.getText().toString()); }
        catch (NumberFormatException ignored) { return 0; } }
    private GameActionCallback callback() { return new GameActionCallback() {
        @Override public void onSuccess() { starsInput.setText(""); tokensInput.setText(""); }
        @Override public void onError(String message) { showInfoDialog(getString(R.string.challenges_title), message); }
    }; }
    @Override protected void onDestroy() { if (repository != null) repository.stop(); super.onDestroy(); }
}
