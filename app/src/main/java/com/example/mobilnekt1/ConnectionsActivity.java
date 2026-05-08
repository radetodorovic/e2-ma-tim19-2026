package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ConnectionsActivity extends BaseKt1Activity {
    private int currentLeft = 0;
    private int matchedCount = 0;
    private int playedTerms = 0;
    private int playerScore = 0;
    private boolean answerLocked = false;
    private Button[] leftButtons;
    private Button[] rightButtons;
    private TextView scoreView;
    private TextView pairsView;
    private String pairsText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections);

        scoreView = findViewById(R.id.text_connections_score);
        pairsView = findViewById(R.id.text_connected_pairs);
        LinearLayout leftContainer = findViewById(R.id.container_left_terms);
        LinearLayout rightContainer = findViewById(R.id.container_right_terms);
        leftButtons = new Button[MockStudentTwoData.CONNECTION_LEFT.length];
        rightButtons = new Button[MockStudentTwoData.CONNECTION_RIGHT.length];

        buildColumn(leftContainer, true);
        buildColumn(rightContainer, false);
        renderSelection();
        renderScore();
    }

    private void buildColumn(LinearLayout container, boolean left) {
        String[] values = left ? MockStudentTwoData.CONNECTION_LEFT : MockStudentTwoData.CONNECTION_RIGHT;
        for (int i = 0; i < values.length; i++) {
            Button button = new Button(this);
            button.setText(values[i]);
            button.setAllCaps(false);
            button.setTextColor(getResources().getColor(R.color.text_primary));
            button.setBackgroundResource(R.drawable.button_outline);
            int index = i;
            button.setOnClickListener(v -> {
                if (!left) {
                    checkCurrentTerm(index);
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            container.addView(button, params);
            if (left) {
                leftButtons[i] = button;
            } else {
                rightButtons[i] = button;
            }
        }
    }

    private void checkCurrentTerm(int selectedRight) {
        if (answerLocked || currentLeft >= leftButtons.length || !rightButtons[selectedRight].isEnabled()) {
            return;
        }

        answerLocked = true;
        boolean correct = MockStudentTwoData.CONNECTION_MATCHES[currentLeft] == selectedRight;
        playedTerms++;
        if (correct) {
            playerScore += 2;
            matchedCount++;
            leftButtons[currentLeft].setEnabled(false);
            rightButtons[selectedRight].setEnabled(false);
            leftButtons[currentLeft].setBackgroundResource(R.drawable.paired_background);
            rightButtons[selectedRight].setBackgroundResource(R.drawable.paired_background);
            pairsText += MockStudentTwoData.CONNECTION_LEFT[currentLeft]
                    + " - " + MockStudentTwoData.CONNECTION_RIGHT[selectedRight] + "\n";
            pairsView.setText(pairsText.trim());
        } else {
            leftButtons[currentLeft].setEnabled(false);
            leftButtons[currentLeft].setBackgroundResource(R.drawable.selected_background);
            rightButtons[selectedRight].setBackgroundResource(R.drawable.selected_background);
            showToast(R.string.incorrect_pair);
        }

        moveToNextLeft();
        answerLocked = false;
        renderScore();
        if (playedTerms == MockStudentTwoData.CONNECTION_LEFT.length) {
            showFinishDialog(getString(R.string.round_result),
                    "Runda Spojnica je zavrsena."
                            + "\nIgrac 1: " + playerScore + " bodova"
                            + "\nIgrac 2: 4 boda"
                            + "\nDruga runda i protivnik su prikazani kao KT1 mock.");
        }
    }

    private void renderSelection() {
        for (int i = 0; i < leftButtons.length; i++) {
            if (leftButtons[i].isEnabled()) {
                leftButtons[i].setBackgroundResource(i == currentLeft
                        ? R.drawable.selected_background
                        : R.drawable.button_outline);
            }
        }
        for (int i = 0; i < rightButtons.length; i++) {
            if (rightButtons[i].isEnabled()) {
                rightButtons[i].setBackgroundResource(R.drawable.button_outline);
            }
        }
    }

    private void moveToNextLeft() {
        currentLeft++;
        while (currentLeft < leftButtons.length && !leftButtons[currentLeft].isEnabled()) {
            currentLeft++;
        }
        renderSelection();
    }

    private void renderScore() {
        scoreView.setText("Igrac 1: " + playerScore + "  |  Igrac 2: 4");
    }
}
