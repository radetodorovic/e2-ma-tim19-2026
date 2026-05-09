package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SkockoActivity extends BaseKt1Activity {
    private int attempt = 0;
    private int slot = 0;
    private final int[][] guesses = new int[6][4];
    private TextView statusView;
    private TextView guessView;
    private LinearLayout historyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void buildSymbols(GridLayout container) {
        for (int i = 0; i < MockStudentThreeData.SKOCKO_SYMBOLS.length; i++) {
            Button button = new Button(this);
            button.setText(MockStudentThreeData.SKOCKO_SYMBOLS[i]);
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
        int exact = 0;
        int misplaced = 0;
        boolean[] usedSolution = new boolean[4];
        boolean[] usedGuess = new boolean[4];

        for (int i = 0; i < 4; i++) {
            if (guesses[attempt][i] == MockStudentThreeData.SKOCKO_COMBINATION[i]) {
                exact++;
                usedSolution[i] = true;
                usedGuess[i] = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (usedGuess[i]) {
                continue;
            }
            for (int j = 0; j < 4; j++) {
                if (!usedSolution[j] && guesses[attempt][i] == MockStudentThreeData.SKOCKO_COMBINATION[j]) {
                    misplaced++;
                    usedSolution[j] = true;
                    break;
                }
            }
        }

        addHistory(exact, misplaced);
        if (exact == 4) {
            int points = attempt < 2 ? 20 : attempt < 4 ? 15 : 10;
            showFinishDialog(getString(R.string.round_result),
                    "Skocko je resen u " + (attempt + 1) + ". pokusaju."
                            + "\nIgrac 1: " + points + " bodova"
                            + "\nIgrac 2: 15 bodova"
                            + "\nDruga runda je KT1 mock.");
            return;
        }

        attempt++;
        slot = 0;
        if (attempt >= 6) {
            showFinishDialog(getString(R.string.round_result),
                    "Skocko nije resen u 6 pokusaja."
                            + "\nProtivnikova sansa od 10 sekundi je prikazana kao KT1 mock.");
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
            text += MockStudentThreeData.SKOCKO_SYMBOLS[guesses[attempt][i]];
            if (i < slot - 1) {
                text += " - ";
            }
        }
        return text.isEmpty() ? "----" : text;
    }

    private void renderStatus() {
        statusView.setText("Runda 1/2 | Timer: 00:30 | Pokusaj " + (attempt + 1) + "/6");
    }

    private void renderGuess() {
        guessView.setText("Trenutna kombinacija: " + buildGuessText());
    }
}
