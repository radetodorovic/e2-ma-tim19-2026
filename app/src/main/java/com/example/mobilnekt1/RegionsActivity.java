package com.example.mobilnekt1;

import android.os.Bundle;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mobilnekt1.regions.data.*;
import com.example.mobilnekt1.regions.domain.RegionStats;
import com.example.mobilnekt1.regions.domain.RegionPlayerPoint;
import com.example.mobilnekt1.regions.presentation.SerbiaMapView;
import java.util.List;

public final class RegionsActivity extends BaseKt1Activity {
    private RegionsRepository repository;
    private LinearLayout container;
    private SerbiaMapView map;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_regions);
        container = findViewById(R.id.container_regions);
        map = findViewById(R.id.region_map);
        findViewById(R.id.button_region_challenges).setOnClickListener(v ->
                startActivity(new Intent(this, ChallengeActivity.class)));
        repository = new RegionsRepository(this);
        repository.listen(new RegionsListener() {
            @Override public void onChanged(List<RegionStats> regions, List<RegionPlayerPoint> points, String mine) {
                map.setPoints(points); render(regions, mine);
            }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.regions_title), message); }
        });
    }

    private void render(List<RegionStats> regions, String mine) {
        container.removeAllViews();
        for (int i = 0; i < regions.size(); i++) {
            RegionStats region = regions.get(i);
            TextView row = new TextView(this);
            String marker = region.name.equals(mine) ? " ★" : "";
            row.setText((i + 1) + ". " + region.name + marker + "\n" + region.monthlyStars
                    + " zvezda | aktivni " + region.activePlayers + "/" + region.registeredPlayers);
            row.setTextSize(17); row.setPadding(14, 18, 14, 18);
            row.setOnClickListener(v -> showInfoDialog(region.name,
                    "Prva mesta: " + region.firstPlaces + "\nDruga mesta: " + region.secondPlaces
                            + "\nTreca mesta: " + region.thirdPlaces + "\nAktivni igraci: "
                            + region.activePlayers + "\nRegistrovani igraci: " + region.registeredPlayers));
            container.addView(row);
        }
    }

    @Override protected void onDestroy() { if (repository != null) repository.stop(); super.onDestroy(); }
}
