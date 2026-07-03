package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mobilnekt1.games.skocko.domain.SkockoEngine;

public class SkockoActivity extends BaseKt1Activity {
    private int attempt = 0;
    private int slot = 0;
    private final int[][] guesses = new int[6][4];
    private TextView statusView;
    private TextView guessView;
    private LinearLayout historyContainer;
    private boolean challengeMode;
    private CountDownTimer timer;
    private long secondsLeft = 30;
    private boolean finished;
    private int[] solution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        challengeMode = getIntent().getBooleanExtra(EXTRA_CHALLENGE_GAME, false);
        solution = SkockoEngine.solutionForSeed(getIntent().getStringExtra(ChallengePlayActivity.EXTRA_CHALLENGE_ID));
        setContentView(R.layout.activity_skocko);

        statusView = findViewById(R.id.text_skocko_status);
        guessView = findViewById(R.id.text_skocko_guess);
        historyContainer = findViewById(R.id.container_skocko_history);
        GridLayout symbolsContainer = findViewById(R.id.container_skocko_symbols);
        Button confirmButton = findViewById(R.id.button_confirm_skocko);
        Button clearButton = findViewById(R.id.button_clear_skocko);

        buildSymbols(symbolsContainer);
        confirmButton.setOnClickListener(v -> confirmAttempt());
        clearButton.setOnClickListener(v -> clearAttempt());
        renderStatus();
        renderGuess();
        timer = new CountDownTimer(30_000, 250) {
            @Override public void onTick(long left) { secondsLeft = (long)Math.ceil(left / 1000.0); renderStatus(); }
            @Override public void onFinish() { secondsLeft = 0; finishSkocko(0, false); }
        }.start();
    }

    private void buildSymbols(GridLayout container) {
        for (int i = 0; i < SkockoEngine.SYMBOLS.length; i++) {
            Button button = new Button(this);
            button.setText(SkockoEngine.SYMBOLS[i]);
            button.setAllCaps(false);
            button.setTextColor(getResources().getColor(R.color.text_primary));
            button.setBackgroundResource(R.drawable.button_outline);
            int index = i;
            button.setOnClickListener(v -> addSymbol(index));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(0, 8, 8, 0);
            container.addView(button, params);
        }
    }

    private void addSymbol(int symbolIndex) {
        if (slot >= 4 || attempt >= 6) {
            return;
        }
        guesses[attempt][slot] = symbolIndex;
        slot++;
        renderGuess();
    }

    private void confirmAttempt() {
        if (slot < 4) {
            showToast(R.string.choose_four_symbols);
            return;
        }
        SkockoEngine.Result evaluated = SkockoEngine.evaluate(solution, guesses[attempt]);
        int exact = evaluated.exact;
        int misplaced = evaluated.misplaced;

        addHistory(exact, misplaced);
        if (exact == 4) {
            int points = SkockoEngine.pointsForAttempt(attempt + 1);
            finishSkocko(points, true);
            return;
        }

        attempt++;
        slot = 0;
        if (attempt >= 6) {
            finishSkocko(0, false);
            return;
        }
        renderStatus();
        renderGuess();
    }

    private void clearAttempt() {
        slot = 0;
        renderGuess();
    }

    private void addHistory(int exact, int misplaced) {
        TextView row = new TextView(this);
        row.setText(buildGuessText() + "  |  Tacno mesto: " + exact + ", pogresno mesto: " + misplaced);
        row.setTextColor(getResources().getColor(R.color.text_primary));
        row.setTextSize(16);
        row.setPadding(0, 6, 0, 6);
        historyContainer.addView(row);
    }

    private String buildGuessText() {
        String text = "";
        for (int i = 0; i < slot; i++) {
            text += SkockoEngine.SYMBOLS[guesses[attempt][i]];
            if (i < slot - 1) {
                text += " - ";
            }
        }
        return text.isEmpty() ? "----" : text;
    }

    private void renderStatus() {
        statusView.setText((challengeMode ? "Samostalna igra" : "Runda 1/2")
                + " | Timer: " + String.format(java.util.Locale.getDefault(), "00:%02d", secondsLeft)
                + " | Pokusaj " + Math.min(attempt + 1, 6) + "/6");
    }

    private void renderGuess() {
        guessView.setText("Trenutna kombinacija: " + buildGuessText());
    }
    private void finishSkocko(int points, boolean solved) {
        if (finished) return; finished = true; if (timer != null) timer.cancel();
        String text = solved ? "Skocko je resen u " + (attempt + 1) + ". pokusaju." : "Skocko nije resen.";
        text += challengeMode ? "\nVasi bodovi: " + points : "\nIgrac 1: " + points + " bodova\nIgrac 2: 15 bodova";
        showGameFinishDialog(getString(R.string.round_result), text, points);
    }
    @Override protected void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
