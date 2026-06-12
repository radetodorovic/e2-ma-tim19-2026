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

    private MatchLobbyViewModel viewModel;
    private EditText codeInput;
    private TextView codeView;
    private TextView statusView;
    private TextView playersView;
    private ProgressBar progressBar;
    private Button createButton;
    private Button joinButton;
    private Button quizButton;
    private Button connectionsButton;
    private Button stepByStepButton;
    private Button myNumberButton;
    private String currentMatchId;

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
        findViewById(R.id.button_lobby_back).setOnClickListener(v -> finish());
    }

    private void bindViews() {
        codeInput = findViewById(R.id.input_match_code);
        codeView = findViewById(R.id.text_match_code);
        statusView = findViewById(R.id.text_match_status);
        playersView = findViewById(R.id.text_match_players);
        progressBar = findViewById(R.id.progress_match);
        createButton = findViewById(R.id.button_create_match);
        joinButton = findViewById(R.id.button_join_match);
        quizButton = findViewById(R.id.button_lobby_quiz);
        connectionsButton = findViewById(R.id.button_lobby_connections);
        stepByStepButton = findViewById(R.id.button_lobby_step_by_step);
        myNumberButton = findViewById(R.id.button_lobby_my_number);
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
            quizButton.setEnabled(false);
            connectionsButton.setEnabled(false);
            stepByStepButton.setEnabled(false);
            myNumberButton.setEnabled(false);
        } else {
            currentMatchId = match.id;
            codeView.setText(getString(R.string.match_code_value, match.id));
            statusView.setText(match.isActive()
                    ? R.string.match_ready : R.string.waiting_for_second_player);
            String secondPlayer = match.player2Name == null
                    ? getString(R.string.waiting_player) : match.player2Name;
            playersView.setText(getString(R.string.match_players_value,
                    match.player1Name, secondPlayer));
            quizButton.setEnabled(match.isActive() && !state.loading);
            connectionsButton.setEnabled(match.isActive() && !state.loading);
            stepByStepButton.setEnabled(match.isActive() && !state.loading);
            myNumberButton.setEnabled(match.isActive() && !state.loading);
        }

        if (state.error != null) {
            showInfoDialog(getString(R.string.match_error_title), state.error);
            viewModel.clearError();
        }
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
        } else {
            return;
        }
        Intent intent = new Intent(this, destination);
        intent.putExtra(EXTRA_MATCH_ID, currentMatchId);
        startActivity(intent);
    }
}
