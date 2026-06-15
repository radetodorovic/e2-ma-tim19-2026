package com.example.mobilnekt1;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mobilnekt1.games.associations.data.AssociationGameListener;
import com.example.mobilnekt1.games.associations.data.AssociationGameRepository;
import com.example.mobilnekt1.games.associations.domain.AssociationGameState;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.match.data.MatchScoreRepository;
import com.example.mobilnekt1.profile.data.StatsRepository;

import java.util.Locale;

public final class MultiplayerAssociationsActivity extends BaseKt1Activity {
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private AssociationGameRepository repository;
    private MatchScoreRepository matchScoreRepository;
    private StatsRepository statsRepository;
    private AssociationGameState state;
    private String matchId;
    private TextView scoreView;
    private TextView solvedView;
    private EditText finalInput;
    private Button finalButton;
    private Button passButton;
    private LinearLayout columnsContainer;
    private Button[][] fieldButtons;
    private EditText[] columnInputs;
    private Button[] columnButtons;
    private boolean resultShown;
    private boolean committed;
    private boolean advancing;
    private boolean roundResultShown;
    private int renderedRound = -1;
    private AlertDialog roundResultDialog;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_associations);
        matchId = getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID);
        if (matchId == null) { finish(); return; }
        scoreView = findViewById(R.id.text_association_score);
        solvedView = findViewById(R.id.text_association_solved);
        finalInput = findViewById(R.id.input_association_final_answer);
        finalButton = findViewById(R.id.button_check_final);
        passButton = findViewById(R.id.button_association_pass);
        columnsContainer = findViewById(R.id.container_association_columns);
        buildColumns();
        repository = new AssociationGameRepository(this);
        matchScoreRepository = new MatchScoreRepository(this);
        statsRepository = new StatsRepository(this);
        finalButton.setOnClickListener(v -> submitFinal());
        passButton.setOnClickListener(v -> repository.passTurn(matchId, callback()));
        repository.listen(matchId, new AssociationGameListener() {
            @Override public void onChanged(AssociationGameState value) {
                state = value;
                advancing = false;
                render();
                if (value.isFinished() && !committed) {
                    committed = true;
                    statsRepository.commitAssociations(matchId, callback());
                    matchScoreRepository.commitGameResult(matchId, "associations", "phase", callback());
                }
            }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.match_error_title), message); }
        });
        repository.initialize(matchId, callback());
        ticker.post(tick);
    }

    private void buildColumns() {
        fieldButtons = new Button[4][4];
        columnInputs = new EditText[4];
        columnButtons = new Button[4];
        for (int column = 0; column < 4; column++) {
            LinearLayout block = new LinearLayout(this);
            block.setOrientation(LinearLayout.VERTICAL);
            block.setBackgroundResource(R.drawable.card_background);
            TextView title = new TextView(this);
            title.setText("Kolona " + (char) ('A' + column));
            title.setTextSize(18); title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.text_primary));
            block.addView(title);
            GridLayout grid = new GridLayout(this); grid.setColumnCount(2);
            for (int row = 0; row < 4; row++) {
                Button field = new Button(this);
                int index = column * 4 + row;
                field.setText("" + (char) ('A' + column) + (row + 1));
                field.setAllCaps(false); field.setBackgroundResource(R.drawable.button_outline);
                field.setOnClickListener(v -> repository.openField(matchId, index, callback()));
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0; params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                grid.addView(field, params); fieldButtons[column][row] = field;
            }
            block.addView(grid);
            EditText input = new EditText(this); input.setHint("Resenje kolone " + (char) ('A' + column));
            input.setSingleLine(true); input.setBackgroundResource(R.drawable.input_background);
            block.addView(input); columnInputs[column] = input;
            Button check = new Button(this); check.setText("Proveri kolonu"); check.setAllCaps(false);
            check.setBackgroundResource(R.drawable.button_primary); check.setTextColor(getResources().getColor(android.R.color.white));
            int selectedColumn = column;
            check.setOnClickListener(v -> repository.submitColumn(matchId, selectedColumn,
                    columnInputs[selectedColumn].getText().toString(), callback()));
            block.addView(check); columnButtons[column] = check;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, 0, 0, 12);
            columnsContainer.addView(block, params);
        }
    }

    private void submitFinal() {
        String answer = finalInput.getText().toString().trim();
        if (answer.isEmpty()) { finalInput.setError(getString(R.string.required_field)); return; }
        repository.submitFinal(matchId, answer, callback());
    }

    private void render() {
        if (state == null) return;
        if (renderedRound != state.round) {
            renderedRound = state.round;
            clearRoundInputs();
            if (state.round == 1) {
                roundResultShown = false;
                dismissRoundResult();
            }
        }
        boolean myTurn = repository.currentUserId() != null && repository.currentUserId().equals(state.activePlayerId);
        long seconds = (long) Math.ceil(Math.max(0, state.deadlineMillis - System.currentTimeMillis()) / 1000.0);
        scoreView.setText(String.format(Locale.getDefault(),
                "Runda %d/2 | %s | %02d:%02d | Bodovi: %d - %d", state.round + 1,
                myTurn ? "vas potez" : "protivnikov potez", seconds / 60, seconds % 60,
                state.player1Score, state.player2Score));
        StringBuilder solved = new StringBuilder();
        for (int column = 0; column < 4; column++) {
            boolean columnSolved = state.isSolved(column);
            for (int row = 0; row < 4; row++) {
                int index = column * 4 + row;
                Button field = fieldButtons[column][row];
                field.setText(state.isOpened(index) && index < state.fields.size()
                        ? state.fields.get(index) : "" + (char) ('A' + column) + (row + 1));
                field.setEnabled(myTurn && !state.isFinished() && state.canOpenField()
                        && !state.isOpened(index) && !columnSolved);
            }
            columnInputs[column].setEnabled(myTurn && !state.isFinished()
                    && state.canGuessColumn() && !columnSolved);
            columnButtons[column].setEnabled(myTurn && !state.isFinished()
                    && state.canGuessColumn() && !columnSolved);
            if (columnSolved && column < state.columnSolutions.size()) {
                columnInputs[column].setText(state.columnSolutions.get(column));
                solved.append((char) ('A' + column)).append(": ").append(state.columnSolutions.get(column)).append('\n');
            } else if (!columnInputs[column].hasFocus()) {
                columnInputs[column].setText("");
            }
        }
        solvedView.setText(solved.length() == 0 ? getString(R.string.no_solved_columns) : solved.toString().trim());
        finalInput.setEnabled(myTurn && !state.isFinished() && state.canGuessFinal());
        finalButton.setEnabled(myTurn && !state.isFinished() && state.canGuessFinal());
        passButton.setEnabled(myTurn && !state.isFinished());
        if (state.isRoundBreak() && !roundResultShown) {
            roundResultShown = true;
            roundResultDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.round_result)
                    .setMessage(getString(R.string.association_round_score,
                            (int) state.player1Score, (int) state.player2Score))
                    .setCancelable(false)
                    .create();
            roundResultDialog.show();
        }
        if (state.isFinished() && !resultShown) {
            resultShown = true;
            showFinishDialog(getString(R.string.game_result), getString(R.string.two_player_score,
                    (int) state.player1Score, (int) state.player2Score));
        }
    }

    private void clearRoundInputs() {
        if (columnInputs != null) {
            for (EditText input : columnInputs) input.setText("");
        }
        if (finalInput != null) finalInput.setText("");
    }

    private void dismissRoundResult() {
        if (roundResultDialog != null) {
            roundResultDialog.dismiss();
            roundResultDialog = null;
        }
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (state != null && !state.isFinished()) {
                render();
                if (state.deadlineMillis <= System.currentTimeMillis() && !advancing) {
                    advancing = true; repository.advanceExpired(matchId, callback());
                }
            }
            ticker.postDelayed(this, 250);
        }
    };
    private GameActionCallback callback() { return new GameActionCallback() {
        @Override public void onSuccess() { }
        @Override public void onError(String message) { advancing = false; showInfoDialog(getString(R.string.match_error_title), message); }
    }; }
    @Override protected void onDestroy() { ticker.removeCallbacksAndMessages(null); dismissRoundResult(); if (repository != null) repository.stopListening(); super.onDestroy(); }
}
