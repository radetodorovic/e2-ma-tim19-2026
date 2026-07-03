package com.example.mobilnekt1;

import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.tournament.data.*;
import com.example.mobilnekt1.tournament.domain.Tournament;
import java.util.List;

public final class TournamentActivity extends BaseKt1Activity {
    private TournamentRepository repository;
    private LinearLayout container;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_tournament);
        container = findViewById(R.id.container_tournaments); repository = new TournamentRepository(this);
        findViewById(R.id.button_create_tournament).setOnClickListener(v -> repository.create(callback()));
        repository.listen(new TournamentListener() {
            @Override public void onChanged(List<Tournament> values, String uid) { render(values, uid); }
            @Override public void onError(String message) { showInfoDialog("Turnir", message); }
        });
    }
    private void render(List<Tournament> values, String uid) {
        container.removeAllViews();
        for (Tournament value : values) {
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16,16,16,16); card.setBackgroundResource(R.drawable.card_background);
            TextView text = new TextView(this); text.setText(description(value)); card.addView(text);
            if ("waiting".equals(value.status) && !value.participantIds.contains(uid)) {
                Button join = new Button(this); join.setText("Pridruzi se (-3 tokena)");
                join.setOnClickListener(v -> repository.join(value.id, callback())); card.addView(join);
            }
            String matchId = matchFor(value, uid);
            if (matchId != null) {
                Button play = new Button(this); play.setText("Odigraj turnirsku partiju");
                play.setOnClickListener(v -> repository.activateMatch(matchId, new GameActionCallback() {
                    @Override public void onSuccess() {
                        Intent intent = new Intent(TournamentActivity.this, MatchLobbyActivity.class);
                        intent.putExtra(MatchLobbyActivity.EXTRA_MATCH_ID, matchId); startActivity(intent);
                    }
                    @Override public void onError(String message) { showInfoDialog("Turnir", message); }
                })); card.addView(play);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2); params.setMargins(0,0,0,12);
            container.addView(card, params);
        }
        if (values.isEmpty()) { TextView empty = new TextView(this); empty.setText(R.string.no_items); container.addView(empty); }
    }
    private String description(Tournament value) {
        StringBuilder text = new StringBuilder("Status: ").append(value.status)
                .append("\nIgraci: ").append(value.participantIds.size()).append("/4");
        if ("semifinals".equals(value.status)) {
            text.append("\n\nPolufinale 1: ").append(pair(value, value.semifinalOneIds));
            text.append("\nPolufinale 2: ").append(pair(value, value.semifinalTwoIds));
            text.append("\nFinale: ceka pobednike");
        } else if ("final".equals(value.status)) {
            text.append("\n\nFinale: ").append(value.participantNames.get(value.semifinalOneWinnerId))
                    .append(" - ").append(value.participantNames.get(value.semifinalTwoWinnerId));
        } else if ("finished".equals(value.status)) {
            text.append("\n\nPobednik: ").append(value.participantNames.get(value.winnerId));
        }
        return text.toString();
    }
    private String pair(Tournament value, List<String> ids) {
        if (ids == null || ids.size() != 2) return "-";
        return value.participantNames.get(ids.get(0)) + " - " + value.participantNames.get(ids.get(1));
    }
    private String matchFor(Tournament value, String uid) {
        if ("semifinals".equals(value.status)) {
            if (value.semifinalOneIds.contains(uid)) return value.semifinalOneMatchId;
            if (value.semifinalTwoIds.contains(uid)) return value.semifinalTwoMatchId;
        }
        if ("final".equals(value.status) && (uid.equals(value.semifinalOneWinnerId)
                || uid.equals(value.semifinalTwoWinnerId))) return value.finalMatchId;
        return null;
    }
    private GameActionCallback callback() { return new GameActionCallback() {
        @Override public void onSuccess() { }
        @Override public void onError(String message) { showInfoDialog("Turnir", message); }
    }; }
    @Override protected void onDestroy() { if (repository != null) repository.stop(); super.onDestroy(); }
}
