package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.match.domain.Match;
import com.example.mobilnekt1.match.presentation.MatchLobbyState;
import com.example.mobilnekt1.match.presentation.MatchLobbyViewModel;

public final class MatchLobbyActivity extends BaseKt1Activity {
    public static final String EXTRA_MATCH_ID = "matchId";
    public static final String GAME_QUIZ = "koZnaZna";
    public static final String GAME_CONNECTIONS = "spojnice";
    public static final String GAME_STEP_BY_STEP = "stepByStep";
    public static final String GAME_MY_NUMBER = "myNumber";
    public static final String GAME_ASSOCIATIONS = "associations";
    public static final String GAME_SKOCKO = "skocko";

    private MatchLobbyViewModel viewModel;
    private EditText codeInput;
    private TextView codeView;
    private TextView statusView;
    private TextView playersView;
    private TextView scoreView;
    private ProgressBar progressBar;
    private Button createButton;
    private Button joinButton;
    private Button quizButton;
    private Button connectionsButton;
    private Button stepByStepButton;
    private Button myNumberButton;
    private Button associationsButton;
    private Button skockoButton;
    private String currentMatchId;
    private boolean finalResultShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_lobby);
        bindViews();

        viewModel = new ViewModelProvider(this).get(MatchLobbyViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.getOpenGame().observe(this, event -> {
            String game = event.getIfNotHandled();
            if (game != null && currentMatchId != null) {
                openGame(game);
            }
        });

        createButton.setOnClickListener(v -> viewModel.createMatch());
        joinButton.setOnClickListener(v -> viewModel.joinMatch(codeInput.getText().toString()));
        quizButton.setOnClickListener(v -> viewModel.selectGame(GAME_QUIZ));
        connectionsButton.setOnClickListener(v -> viewModel.selectGame(GAME_CONNECTIONS));
        stepByStepButton.setOnClickListener(v -> viewModel.selectGame(GAME_STEP_BY_STEP));
        myNumberButton.setOnClickListener(v -> viewModel.selectGame(GAME_MY_NUMBER));
        associationsButton.setOnClickListener(v -> viewModel.selectGame(GAME_ASSOCIATIONS));
        skockoButton.setOnClickListener(v -> viewModel.selectGame(GAME_SKOCKO));
        findViewById(R.id.button_lobby_back).setOnClickListener(v -> finish());
    }

    private void bindViews() {
        codeInput = findViewById(R.id.input_match_code);
        codeView = findViewById(R.id.text_match_code);
        statusView = findViewById(R.id.text_match_status);
        playersView = findViewById(R.id.text_match_players);
        scoreView = findViewById(R.id.text_match_score);
        progressBar = findViewById(R.id.progress_match);
        createButton = findViewById(R.id.button_create_match);
        joinButton = findViewById(R.id.button_join_match);
        quizButton = findViewById(R.id.button_lobby_quiz);
        connectionsButton = findViewById(R.id.button_lobby_connections);
        stepByStepButton = findViewById(R.id.button_lobby_step_by_step);
        myNumberButton = findViewById(R.id.button_lobby_my_number);
        associationsButton = findViewById(R.id.button_lobby_associations);
        skockoButton = findViewById(R.id.button_lobby_skocko);
    }

    private void render(MatchLobbyState state) {
        progressBar.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        createButton.setEnabled(!state.loading && state.match == null);
        joinButton.setEnabled(!state.loading && state.match == null);
        codeInput.setEnabled(!state.loading && state.match == null);

        Match match = state.match;
        if (match == null) {
            currentMatchId = null;
            codeView.setText(R.string.no_active_match);
            statusView.setText(R.string.create_or_join_match);
            playersView.setText(R.string.waiting_for_match);
            scoreView.setText(R.string.match_score_empty);
            quizButton.setEnabled(false);
            connectionsButton.setEnabled(false);
            stepByStepButton.setEnabled(false);
            myNumberButton.setEnabled(false);
            associationsButton.setEnabled(false);
            skockoButton.setEnabled(false);
        } else {
            currentMatchId = match.id;
            codeView.setText(getString(R.string.match_code_value, match.id));
            statusView.setText(match.isActive()
                    ? R.string.match_ready : R.string.waiting_for_second_player);
            String secondPlayer = match.player2Name == null
                    ? getString(R.string.waiting_player) : match.player2Name;
            playersView.setText(getString(R.string.match_players_value,
                    match.player1Name, secondPlayer));
            int completed = match.completedGames == null ? 0 : match.completedGames.size();
            scoreView.setText(getString(R.string.match_total_score,
                    match.player1Name, (int) match.player1Score,
                    secondPlayer, (int) match.player2Score, completed, 6));
            quizButton.setEnabled(canStart(match, GAME_QUIZ, state.loading));
            connectionsButton.setEnabled(canStart(match, GAME_CONNECTIONS, state.loading));
            stepByStepButton.setEnabled(canStart(match, GAME_STEP_BY_STEP, state.loading));
            myNumberButton.setEnabled(canStart(match, GAME_MY_NUMBER, state.loading));
            associationsButton.setEnabled(canStart(match, GAME_ASSOCIATIONS, state.loading));
            skockoButton.setEnabled(canStart(match, GAME_SKOCKO, state.loading));
            if (match.isFinished() && !finalResultShown) {
                finalResultShown = true;
                showInfoDialog(getString(R.string.final_match_result_title), finalResult(match));
            }
        }

        if (state.error != null) {
            showInfoDialog(getString(R.string.match_error_title), state.error);
            viewModel.clearError();
        }
    }

    private boolean canStart(Match match, String game, boolean loading) {
        return match.isActive() && !loading && !match.isGameCompleted(game);
    }

    private String finalResult(Match match) {
        String outcome;
        if (match.winnerId == null) {
            outcome = getString(R.string.final_match_draw);
        } else if (match.winnerId.equals(match.player1Id)) {
            outcome = getString(R.string.final_match_winner, match.player1Name);
        } else {
            outcome = getString(R.string.final_match_winner, match.player2Name);
        }
        return getString(R.string.final_match_score,
                match.player1Name, (int) match.player1Score,
                match.player2Name, (int) match.player2Score, outcome);
    }

    private void openGame(String game) {
        Class<?> destination;
        if (GAME_QUIZ.equals(game)) {
            destination = MultiplayerQuizActivity.class;
        } else if (GAME_CONNECTIONS.equals(game)) {
            destination = MultiplayerConnectionsActivity.class;
        } else if (GAME_STEP_BY_STEP.equals(game)) {
            destination = MultiplayerStepByStepActivity.class;
        } else if (GAME_MY_NUMBER.equals(game)) {
            destination = MultiplayerMyNumberActivity.class;
        } else if (GAME_ASSOCIATIONS.equals(game)) {
            destination = MultiplayerAssociationsActivity.class;
        } else if (GAME_SKOCKO.equals(game)) {
            destination = MultiplayerSkockoActivity.class;
        } else {
            return;
        }
        Intent intent = new Intent(this, destination);
        intent.putExtra(EXTRA_MATCH_ID, currentMatchId);
        startActivity(intent);
    }
}
