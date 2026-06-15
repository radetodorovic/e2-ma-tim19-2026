package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.example.mobilnekt1.profile.domain.PlayerStats;
import com.example.mobilnekt1.profile.presentation.ProfileViewModel;
import java.util.Locale;

public class StatisticsActivity extends BaseKt1Activity {
    private LinearLayout container;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        container = findViewById(R.id.container_statistics);
        ProfileViewModel viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getState().observe(this, state -> render(state.stats));
        viewModel.getError().observe(this, event -> {
            String message = event.getIfNotHandled();
            if (message != null) showInfoDialog(getString(R.string.profile_error_title), message);
        });
    }

    private void render(PlayerStats stats) {
        while (container.getChildCount() > 1) container.removeViewAt(1);
        addCard(getString(R.string.average_scores_title) + "\n" +
                "Ko zna zna: " + average(stats, "koZnaZna") + "\n" +
                "Spojnice: " + average(stats, "spojnice") + "\n" +
                "Moj broj: " + average(stats, "myNumber") + "\n" +
                "Korak po korak: " + average(stats, "stepByStep") + "\n" +
                "Asocijacije: " + average(stats, "associations") + "\n" +
                "Skocko: " + average(stats, "skocko"));
        addCard(getString(R.string.quiz_correct_wrong_value, stats.koZnaZnaCorrect, stats.koZnaZnaWrong));
        double connectionPercent = stats.spojniceTotalPairs == 0 ? 0
                : stats.spojniceCorrectPairs * 100.0 / stats.spojniceTotalPairs;
        addCard(getString(R.string.connections_percent_value, connectionPercent));
        double myNumberPercent = stats.myNumberTotalRounds == 0 ? 0
                : stats.myNumberExactRounds * 100.0 / stats.myNumberTotalRounds;
        addCard(getString(R.string.my_number_percent_value, myNumberPercent,
                stats.myNumberExactRounds, stats.myNumberTotalRounds));
        StringBuilder stepStats = new StringBuilder(getString(R.string.step_percent_title));
        for (int step = 1; step <= 7; step++) {
            long solved = stats.stepSolvedByHint == null ? 0
                    : stats.stepSolvedByHint.containsKey(String.valueOf(step))
                    ? stats.stepSolvedByHint.get(String.valueOf(step)) : 0L;
            double percent = stats.stepRoundsPlayed == 0 ? 0
                    : solved * 100.0 / stats.stepRoundsPlayed;
            stepStats.append('\n').append(getString(R.string.step_percent_row, step, percent));
        }
        addCard(stepStats.toString());
        double associationPercent = stats.associationsTotal == 0 ? 0
                : stats.associationsSolved * 100.0 / stats.associationsTotal;
        addCard(String.format(Locale.getDefault(), "Asocijacije: %.1f%% resenih (%d/%d)",
                associationPercent, stats.associationsSolved, stats.associationsTotal));
        StringBuilder skockoStats = new StringBuilder("Skocko: procenat pogodaka po pokusaju");
        for (int attempt = 1; attempt <= 6; attempt++) {
            long solved = stats.skockoSolvedByAttempt == null ? 0
                    : stats.skockoSolvedByAttempt.containsKey(String.valueOf(attempt))
                    ? stats.skockoSolvedByAttempt.get(String.valueOf(attempt)) : 0L;
            double percent = stats.skockoRoundsPlayed == 0 ? 0 : solved * 100.0 / stats.skockoRoundsPlayed;
            skockoStats.append('\n').append(String.format(Locale.getDefault(),
                    "Pokusaj %d: %.1f%%", attempt, percent));
        }
        addCard(skockoStats.toString());
        Button back = new Button(this);
        back.setText(R.string.back); back.setAllCaps(false); back.setOnClickListener(v -> finish());
        container.addView(back);
    }

    private String average(PlayerStats stats, String game) {
        Double value = stats.averageScoreByGame == null ? null : stats.averageScoreByGame.get(game);
        return String.format(Locale.getDefault(), "%.1f", value == null ? 0 : value);
    }
    private void addCard(String text) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(16);
        view.setTextColor(getResources().getColor(R.color.text_primary));
        view.setBackgroundResource(R.drawable.card_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 10); container.addView(view, params);
    }
}
