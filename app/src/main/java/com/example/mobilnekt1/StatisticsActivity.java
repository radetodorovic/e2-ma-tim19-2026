package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatisticsActivity extends BaseKt1Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        LinearLayout container = findViewById(R.id.container_statistics);
        for (String statistic : MockStudentTwoData.STATISTICS) {
            TextView view = new TextView(this);
            view.setText(statistic);
            view.setTextColor(getResources().getColor(R.color.text_primary));
            view.setTextSize(16);
            view.setBackgroundResource(R.drawable.card_background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 10);
            container.addView(view, params);
        }

        Button backButton = new Button(this);
        backButton.setText(R.string.back);
        backButton.setAllCaps(false);
        backButton.setTextColor(getResources().getColor(R.color.accent));
        backButton.setBackgroundResource(R.drawable.button_outline);
        backButton.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 0);
        container.addView(backButton, params);
    }
}
