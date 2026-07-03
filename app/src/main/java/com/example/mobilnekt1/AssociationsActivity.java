package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.content.GameContentRepository;

public class AssociationsActivity extends BaseKt1Activity {
    private final boolean[][] opened = new boolean[4][4];
    private final boolean[] solvedColumns = new boolean[4];
    private int openedFields = 0;
    private int score = 0;
    private Button[][] fieldButtons;
    private EditText[] columnInputs;
    private TextView scoreView;
    private TextView solvedView;
    private EditText finalAnswerInput;
    private boolean challengeMode;
    private CountDownTimer timer;
    private long secondsLeft = 120;
    private boolean finished;
    private GameContentRepository.AssociationPuzzle puzzle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        challengeMode = getIntent().getBooleanExtra(EXTRA_CHALLENGE_GAME, false);
        setContentView(R.layout.activity_associations);

        scoreView = findViewById(R.id.text_association_score);
        solvedView = findViewById(R.id.text_association_solved);
        finalAnswerInput = findViewById(R.id.input_association_final_answer);
        Button finalButton = findViewById(R.id.button_check_final);
        LinearLayout container = findViewById(R.id.container_association_columns);

        fieldButtons = new Button[4][4];
        columnInputs = new EditText[4];
        finalButton.setEnabled(false);
        new GameContentRepository(FirebaseProvider.getInstance(this).getFirestore())
                .loadAssociationPuzzle(new GameContentRepository.Callback<GameContentRepository.AssociationPuzzle>() {
                    @Override public void onSuccess(GameContentRepository.AssociationPuzzle value) {
                        puzzle = value; buildColumns(container); finalButton.setEnabled(true);
                        finalButton.setOnClickListener(v -> checkFinalSolution()); renderScore(); startTimer();
                    }
                    @Override public void onError(String message) {
                        showFinishDialog(getString(R.string.associations_title), message);
                    }
                });
    }
    private void startTimer() {
        timer = new CountDownTimer(120_000, 250) {
            @Override public void onTick(long left) { secondsLeft = (long)Math.ceil(left / 1000.0); renderScore(); }
            @Override public void onFinish() { secondsLeft = 0; finishAssociations(false); }
        }.start();
    }

    private void buildColumns(LinearLayout container) {
        String[] labels = {"A", "B", "C", "D"};
        for (int column = 0; column < 4; column++) {
            LinearLayout columnLayout = new LinearLayout(this);
            columnLayout.setOrientation(LinearLayout.VERTICAL);
            columnLayout.setBackgroundResource(R.drawable.card_background);

            TextView title = new TextView(this);
            title.setText("Kolona " + labels[column]);
            title.setTextColor(getResources().getColor(R.color.text_primary));
            title.setTextSize(18);
            title.setTypeface(null, Typeface.BOLD);
            columnLayout.addView(title);

            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(2);
            for (int row = 0; row < 4; row++) {
                Button button = new Button(this);
                button.setText(labels[column] + (row + 1));
                button.setAllCaps(false);
                button.setTextColor(getResources().getColor(R.color.text_primary));
                button.setBackgroundResource(R.drawable.button_outline);
                int currentColumn = column;
                int currentRow = row;
                button.setOnClickListener(v -> openField(currentColumn, currentRow));
                GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
                gridParams.width = 0;
                gridParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                gridParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                gridParams.setMargins(0, 8, 8, 0);
                grid.addView(button, gridParams);
                fieldButtons[column][row] = button;
            }
            columnLayout.addView(grid);

            EditText input = new EditText(this);
            input.setHint("Resenje kolone " + labels[column]);
            input.setSingleLine(true);
            input.setMinHeight(48);
            input.setBackgroundResource(R.drawable.input_background);
            input.setTextColor(getResources().getColor(R.color.text_primary));
            input.setHintTextColor(getResources().getColor(R.color.text_secondary));
            columnLayout.addView(input);
            columnInputs[column] = input;

            Button checkButton = new Button(this);
            checkButton.setText("Proveri kolonu " + labels[column]);
            checkButton.setAllCaps(false);
            checkButton.setTextColor(getResources().getColor(android.R.color.white));
            checkButton.setBackgroundResource(R.drawable.button_primary);
            int currentColumn = column;
            checkButton.setOnClickListener(v -> checkColumnSolution(currentColumn));
            columnLayout.addView(checkButton);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 12);
            container.addView(columnLayout, params);
        }
    }

    private void openField(int column, int row) {
        if (opened[column][row] || solvedColumns[column]) {
            return;
        }
        opened[column][row] = true;
        openedFields++;
        fieldButtons[column][row].setText(puzzle.fields.get(column).get(row));
        fieldButtons[column][row].setBackgroundResource(R.drawable.selected_background);
        renderScore();
    }

    private void checkColumnSolution(int column) {
        String answer = columnInputs[column].getText().toString().trim();
        if (answer.isEmpty()) {
            showToast(R.string.empty_fields);
            return;
        }
        if (solvedColumns[column]) {
            showToast(R.string.column_already_solved);
            return;
        }
        if (answer.equalsIgnoreCase(puzzle.columnSolutions.get(column))) {
            solvedColumns[column] = true;
            int unopened = 0;
            for (int row = 0; row < 4; row++) {
                if (!opened[column][row]) {
                    unopened++;
                    openedFields++;
                }
                opened[column][row] = true;
                fieldButtons[column][row].setText(puzzle.fields.get(column).get(row));
                fieldButtons[column][row].setEnabled(false);
                fieldButtons[column][row].setBackgroundResource(R.drawable.paired_background);
            }
            score += 2 + unopened;
            columnInputs[column].setText(puzzle.columnSolutions.get(column));
            columnInputs[column].setEnabled(false);
            renderScore();
            renderSolved();
            return;
        }
        showToast(R.string.incorrect_answer);
    }

    private void checkFinalSolution() {
        String answer = finalAnswerInput.getText().toString().trim();
        if (answer.isEmpty()) {
            showToast(R.string.empty_fields);
            return;
        }
        if (answer.equalsIgnoreCase(puzzle.finalSolution)) {
            int unopenedColumns = 0;
            for (boolean solvedColumn : solvedColumns) {
                if (!solvedColumn) {
                    unopenedColumns++;
                }
            }
            score += 7 + unopenedColumns * 6;
            finishAssociations(true);
        } else {
            showToast(R.string.incorrect_answer);
        }
    }

    private void renderScore() {
        scoreView.setText((challengeMode ? "Samostalna igra" : "Runda 1/2")
                + " | Timer: " + String.format(java.util.Locale.getDefault(), "%02d:%02d", secondsLeft / 60, secondsLeft % 60)
                + " | Otvoreno: " + openedFields + " | Bodovi: " + score);
    }

    private void renderSolved() {
        String text = "";
        for (int i = 0; i < solvedColumns.length; i++) {
            if (solvedColumns[i]) {
                text += (char) ('A' + i) + ": "
                        + puzzle.columnSolutions.get(i) + "\n";
            }
        }
        solvedView.setText(text.trim().isEmpty() ? getString(R.string.no_solved_columns) : text.trim());
    }
    private void finishAssociations(boolean solved) {
        if (finished) return; finished = true; if (timer != null) timer.cancel();
        String text = (solved ? "Konacno resenje: " + puzzle.finalSolution : "Vreme je isteklo.")
                + (challengeMode ? "\nVasi bodovi: " + score : "\nIgrac 1: " + score + " bodova\nIgrac 2: 18 bodova");
        showGameFinishDialog(getString(R.string.round_result), text, score);
    }
    @Override protected void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
