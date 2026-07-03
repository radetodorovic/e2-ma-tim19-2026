package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.content.GameContentRepository;
import java.util.List;

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
    private boolean challengeMode;
    private CountDownTimer timer;
    private boolean finished;
    private GameContentRepository.ConnectionsPuzzle puzzle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        challengeMode = getIntent().getBooleanExtra(EXTRA_CHALLENGE_GAME, false);
        setContentView(R.layout.activity_connections);

        scoreView = findViewById(R.id.text_connections_score);
        pairsView = findViewById(R.id.text_connected_pairs);
        LinearLayout leftContainer = findViewById(R.id.container_left_terms);
        LinearLayout rightContainer = findViewById(R.id.container_right_terms);
        new GameContentRepository(FirebaseProvider.getInstance(this).getFirestore())
                .loadConnectionsPuzzles(new GameContentRepository.Callback<List<GameContentRepository.ConnectionsPuzzle>>() {
                    @Override public void onSuccess(List<GameContentRepository.ConnectionsPuzzle> values) {
                        puzzle = values.get(0); leftButtons = new Button[puzzle.leftItems.size()];
                        rightButtons = new Button[puzzle.rightItems.size()]; buildColumn(leftContainer, true);
                        buildColumn(rightContainer, false); renderSelection(); renderScore(); startTimer();
                    }
                    @Override public void onError(String message) {
                        showFinishDialog(getString(R.string.connections_title), message);
                    }
                });
    }

    private void buildColumn(LinearLayout container, boolean left) {
        List<String> values = left ? puzzle.leftItems : puzzle.rightItems;
        for (int i = 0; i < values.size(); i++) {
            Button button = new Button(this);
            button.setText(values.get(i));
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
        boolean correct = puzzle.correctMatches.get(currentLeft) == selectedRight;
        playedTerms++;
        if (correct) {
            playerScore += 2;
            matchedCount++;
            leftButtons[currentLeft].setEnabled(false);
            rightButtons[selectedRight].setEnabled(false);
            leftButtons[currentLeft].setBackgroundResource(R.drawable.paired_background);
            rightButtons[selectedRight].setBackgroundResource(R.drawable.paired_background);
            pairsText += puzzle.leftItems.get(currentLeft)
                    + " - " + puzzle.rightItems.get(selectedRight) + "\n";
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
        if (playedTerms == puzzle.leftItems.size()) {
            finishConnections();
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
        scoreView.setText(scoreText());
    }
    private String scoreText() { return challengeMode ? "Bodovi: " + playerScore
            : "Igrac 1: " + playerScore + "  |  Igrac 2: 4"; }
    private String seconds(long left) { return String.format(java.util.Locale.getDefault(), "00:%02d", (int)Math.ceil(left / 1000.0)); }
    private void startTimer() {
        timer = new CountDownTimer(30_000, 250) {
            @Override public void onTick(long left) { scoreView.setText(scoreText() + " | Timer: " + seconds(left)); }
            @Override public void onFinish() { finishConnections(); }
        }.start();
    }
    private void finishConnections() {
        if (finished) return; finished = true; if (timer != null) timer.cancel();
        String text = challengeMode ? "Spojnice su zavrsene.\nVasi bodovi: " + playerScore
                : "Spojnice su zavrsene.\nIgrac 1: " + playerScore + "\nIgrac 2: 4";
        showGameFinishDialog(getString(R.string.round_result), text, playerScore);
    }
    @Override protected void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
