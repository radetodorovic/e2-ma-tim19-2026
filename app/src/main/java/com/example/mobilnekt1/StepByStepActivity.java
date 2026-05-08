package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StepByStepActivity extends BaseKt1Activity {
    private int openedSteps = 1;
    private int points = 20;
    private LinearLayout hintsContainer;
    private TextView pointsView;
    private EditText answerInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_by_step);

        hintsContainer = findViewById(R.id.container_hints);
        pointsView = findViewById(R.id.text_step_points);
        answerInput = findViewById(R.id.input_step_answer);
        Button confirmButton = findViewById(R.id.button_confirm_step_answer);
        Button nextButton = findViewById(R.id.button_next_step);
        TextView targetPlaceholder = findViewById(R.id.text_step_target_placeholder);
        targetPlaceholder.setText("Mock pojam: " + MockGameData.STEP_TARGET);

        confirmButton.setOnClickListener(v -> showRoundResult());
        nextButton.setOnClickListener(v -> openNextStep());
        renderHints();
    }

    private void openNextStep() {
        if (openedSteps < MockGameData.STEP_HINTS.length) {
            openedSteps++;
            points = Math.max(8, 20 - ((openedSteps - 1) * 2));
            renderHints();
        } else {
            showInfoDialog(getString(R.string.step_by_step_title),
                    "Svi koraci su otvoreni. Protivnik bi u pravoj igri imao sansu za 5 bodova.");
        }
    }

    private void renderHints() {
        hintsContainer.removeAllViews();
        for (int i = 0; i < MockGameData.STEP_HINTS.length; i++) {
            TextView hintView = new TextView(this);
            hintView.setTextSize(16);
            hintView.setTextColor(getResources().getColor(R.color.text_primary));
            hintView.setPadding(16, 12, 16, 12);
            if (i < openedSteps) {
                hintView.setText((i + 1) + ". " + MockGameData.STEP_HINTS[i]);
                hintView.setBackgroundResource(R.drawable.card_background);
            } else {
                hintView.setText((i + 1) + ". Sakriven korak");
                hintView.setBackgroundResource(R.drawable.input_background);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 10);
            hintsContainer.addView(hintView, params);
        }
        pointsView.setText("Moguci bodovi: " + points);
    }

    private void showRoundResult() {
        String answer = answerInput.getText().toString().trim();
        if (answer.isEmpty()) {
            showToast(R.string.empty_fields);
            return;
        }
        if (answer.equalsIgnoreCase(MockGameData.STEP_TARGET)) {
            String message = "Tacan odgovor: " + answer
                    + "\nOsvojeni bodovi u prototipu: " + points
                    + "\nRunda 1/2 je zavrsena.";
            showFinishDialog(getString(R.string.round_result), message);
            return;
        }

        if (openedSteps < MockGameData.STEP_HINTS.length) {
            showInfoDialog(getString(R.string.step_by_step_title),
                    "Odgovor nije tacan. Otvorite sledeci korak i pokusajte ponovo.");
            answerInput.setText("");
            return;
        }

        String message = "Unet odgovor: " + answer
                + "\nMock tacan pojam: " + MockGameData.STEP_TARGET
                + "\nNema vise koraka. Protivnik bi u pravoj igri imao sansu za 5 bodova."
                + "\nRunda 1/2 je zavrsena.";
        showFinishDialog(getString(R.string.round_result), message);
    }
}
