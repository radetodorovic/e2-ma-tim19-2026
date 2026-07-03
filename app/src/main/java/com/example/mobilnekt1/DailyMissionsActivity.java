package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.TextView;
import com.example.mobilnekt1.missions.data.DailyMissionsRepository;
import com.example.mobilnekt1.missions.data.DailyMissionsListener;
import com.example.mobilnekt1.missions.domain.DailyMissions;

public final class DailyMissionsActivity extends BaseKt1Activity {
    private DailyMissionsRepository repository;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_daily_missions);
        repository = new DailyMissionsRepository(this);
        repository.listen(new DailyMissionsListener() {
            @Override public void onChanged(DailyMissions missions) { render(missions); }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.daily_missions_title), message); }
        });
    }
    private void render(DailyMissions value) {
        set(R.id.mission_win_match, value.winMatch, "Pobedi regularnu partiju");
        set(R.id.mission_send_chat, value.sendChat, "Posalji poruku u regionalni cet");
        set(R.id.mission_play_friendly, value.playFriendly, "Odigraj prijateljsku partiju");
        set(R.id.mission_win_tournament, value.winTournament, "Pobedi partiju u turniru");
        ((TextView)findViewById(R.id.text_all_missions_reward)).setText(value.allRewarded
                ? "Sve misije su zavrsene: +2 tokena i dodatne +3 zvezde"
                : "Svaka misija: +3 zvezde. Sve cetiri: +2 tokena i dodatne +3 zvezde.");
    }
    private void set(int id, boolean done, String title) {
        ((TextView)findViewById(id)).setText((done ? "✓ " : "○ ") + title + (done ? "  (+3)" : ""));
    }
    @Override protected void onDestroy() { if (repository != null) repository.stop(); super.onDestroy(); }
}
