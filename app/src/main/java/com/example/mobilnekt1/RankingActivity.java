package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mobilnekt1.ranking.data.RankingListener;
import com.example.mobilnekt1.ranking.data.RankingRepository;
import com.example.mobilnekt1.ranking.domain.RankingEntry;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public final class RankingActivity extends BaseKt1Activity {
    private RankingRepository repository;
    private LinearLayout weeklyView, monthlyView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_ranking);
        weeklyView = findViewById(R.id.container_weekly_ranking);
        monthlyView = findViewById(R.id.container_monthly_ranking);
        ((TextView) findViewById(R.id.text_weekly_cycle)).setText(weeklyCycle());
        ((TextView) findViewById(R.id.text_monthly_cycle)).setText(monthlyCycle());
        repository = new RankingRepository(this);
        repository.listen(new RankingListener() {
            @Override public void onChanged(List<RankingEntry> weekly, List<RankingEntry> monthly) {
                render(weeklyView, weekly); render(monthlyView, monthly);
            }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.ranking_title), message); }
        });
    }

    @Override protected void onDestroy() { if (repository != null) repository.stop(); super.onDestroy(); }

    private void render(LinearLayout container, List<RankingEntry> entries) {
        container.removeAllViews();
        if (entries.isEmpty()) { addRow(container, getString(R.string.no_items)); return; }
        for (int i = 0; i < entries.size(); i++) {
            RankingEntry e = entries.get(i);
            addRow(container, (i + 1) + ". " + e.username + "  |  Liga " + e.league + "  |  " + e.stars + " zvezda");
        }
    }

    private void addRow(LinearLayout container, String text) {
        TextView row = new TextView(this); row.setText(text); row.setTextSize(16); row.setPadding(8, 14, 8, 14); container.addView(row);
    }

    private String weeklyCycle() {
        Calendar start = Calendar.getInstance();
        int day = start.get(Calendar.DAY_OF_WEEK);
        int fromMonday = (day + 5) % 7;
        start.add(Calendar.DAY_OF_MONTH, -fromMonday);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_MONTH, 6);
        return range(start, end);
    }

    private String monthlyCycle() {
        Calendar start = Calendar.getInstance(); start.set(Calendar.DAY_OF_MONTH, 1);
        Calendar end = (Calendar) start.clone(); end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        return range(start, end);
    }

    private String range(Calendar start, Calendar end) {
        DateFormat format = android.text.format.DateFormat.getMediumDateFormat(this);
        return format.format(start.getTime()) + " - " + format.format(end.getTime());
    }
}
