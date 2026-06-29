package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.activity.OnBackPressedCallback;

import com.example.mobilnekt1.match.domain.Match;
import com.example.mobilnekt1.match.domain.MatchGameSequence;
import com.example.mobilnekt1.match.presentation.MatchLobbyState;
import com.example.mobilnekt1.match.presentation.MatchLobbyViewModel;
import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.friends.data.FriendsRepository;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

public final class MatchLobbyActivity extends BaseKt1Activity {
    public static final String EXTRA_MATCH_ID = "matchId";
    public static final String EXTRA_MATCH_INVITE_ID = "matchInviteId";
    public static final String GAME_QUIZ = MatchGameSequence.QUIZ;
    public static final String GAME_CONNECTIONS = MatchGameSequence.CONNECTIONS;
    public static final String GAME_STEP_BY_STEP = MatchGameSequence.STEP_BY_STEP;
    public static final String GAME_MY_NUMBER = MatchGameSequence.MY_NUMBER;
    public static final String GAME_ASSOCIATIONS = MatchGameSequence.ASSOCIATIONS;
    public static final String GAME_SKOCKO = MatchGameSequence.SKOCKO;

    private MatchLobbyViewModel viewModel;
    private EditText codeInput;
    private TextView codeView;
    private TextView statusView;
    private TextView playersView;
    private TextView scoreView;
    private TextView resourcesView;
    private ProgressBar progressBar;
    private Button createButton;
    private Button randomButton;
    private Button joinButton;
    private Button quizButton;
    private Button connectionsButton;
    private Button stepByStepButton;
    private Button myNumberButton;
    private Button associationsButton;
    private Button skockoButton;
    private String currentMatchId;
    private String currentInviteId;
    private Match currentMatch;
    private boolean finalResultShown;
    private ListenerRegistration resourcesRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_lobby);
        bindViews();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveLobby(); }
        });

        viewModel = new ViewModelProvider(this).get(MatchLobbyViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.getOpenGame().observe(this, event -> {
            String game = event.getIfNotHandled();
            if (game != null && currentMatchId != null) {
                openGame(game);
            }
        });

        createButton.setOnClickListener(v -> viewModel.createMatch());
        randomButton.setOnClickListener(v -> viewModel.findRandomMatch());
        joinButton.setOnClickListener(v -> viewModel.joinMatch(codeInput.getText().toString()));
        quizButton.setOnClickListener(v -> viewModel.selectGame(GAME_QUIZ));
        connectionsButton.setOnClickListener(v -> viewModel.selectGame(GAME_CONNECTIONS));
        stepByStepButton.setOnClickListener(v -> viewModel.selectGame(GAME_STEP_BY_STEP));
        myNumberButton.setOnClickListener(v -> viewModel.selectGame(GAME_MY_NUMBER));
        associationsButton.setOnClickListener(v -> viewModel.selectGame(GAME_ASSOCIATIONS));
        skockoButton.setOnClickListener(v -> viewModel.selectGame(GAME_SKOCKO));
        findViewById(R.id.button_lobby_back).setOnClickListener(v -> leaveLobby());
        currentInviteId = getIntent().getStringExtra(EXTRA_MATCH_INVITE_ID);
        String resumeMatchId = getIntent().getStringExtra(EXTRA_MATCH_ID);
        if (resumeMatchId != null) viewModel.resumeMatch(resumeMatchId);
    }

    private void bindViews() {
        codeInput = findViewById(R.id.input_match_code);
        codeView = findViewById(R.id.text_match_code);
        statusView = findViewById(R.id.text_match_status);
        playersView = findViewById(R.id.text_match_players);
        scoreView = findViewById(R.id.text_match_score);
        resourcesView = findViewById(R.id.text_lobby_resources);
        progressBar = findViewById(R.id.progress_match);
        createButton = findViewById(R.id.button_create_match);
        randomButton = findViewById(R.id.button_random_match);
        joinButton = findViewById(R.id.button_join_match);
        quizButton = findViewById(R.id.button_lobby_quiz);
        connectionsButton = findViewById(R.id.button_lobby_connections);
        stepByStepButton = findViewById(R.id.button_lobby_step_by_step);
        myNumberButton = findViewById(R.id.button_lobby_my_number);
        associationsButton = findViewById(R.id.button_lobby_associations);
        skockoButton = findViewById(R.id.button_lobby_skocko);
        quizButton.setVisibility(View.GONE);
        connectionsButton.setVisibility(View.GONE);
        stepByStepButton.setVisibility(View.GONE);
        myNumberButton.setVisibility(View.GONE);
        associationsButton.setVisibility(View.GONE);
        skockoButton.setVisibility(View.GONE);
    }

    private void render(MatchLobbyState state) {
        progressBar.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        createButton.setEnabled(!state.loading && state.match == null);
        randomButton.setEnabled(!state.loading && state.match == null);
        joinButton.setEnabled(!state.loading && state.match == null);
        codeInput.setEnabled(!state.loading && state.match == null);

        Match match = state.match;
        currentMatch = match;
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
            if (match.isAbandoned()) {
                statusView.setText(R.string.match_abandoned);
            } else if (match.abandonedByUserId != null) {
                FirebaseUser user = FirebaseProvider.getInstance(this).getCurrentUser();
                statusView.setText(user != null && user.getUid().equals(match.abandonedByUserId)
                        ? R.string.you_abandoned_match : R.string.opponent_abandoned_match);
            } else if (match.isFinished()) {
                statusView.setText(R.string.game_finished);
            } else {
                statusView.setText(match.isActive()
                        ? R.string.match_ready : R.string.waiting_for_second_player);
            }
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
            if (match.isTerminal() && !finalResultShown) {
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
        if (match.isAbandoned() && match.player2Id == null) {
            outcome = getString(R.string.final_match_cancelled);
        } else if (match.winnerId == null) {
            outcome = getString(R.string.final_match_draw);
        } else if (match.winnerId.equals(match.player1Id)) {
            outcome = getString(R.string.final_match_winner, match.player1Name);
        } else {
            outcome = getString(R.string.final_match_winner, match.player2Name);
        }
        String secondPlayer = match.player2Name == null
                ? getString(R.string.waiting_player) : match.player2Name;
        String score = getString(R.string.final_match_score,
                match.player1Name, (int) match.player1Score,
                secondPlayer, (int) match.player2Score, outcome);
        return score + getString(R.string.final_match_rewards,
                match.player1Name, match.player1StarDelta, match.player1TokenReward,
                secondPlayer, match.player2StarDelta, match.player2TokenReward);
    }

    @Override protected void onStart() {
        super.onStart();
        FirebaseProvider firebase = FirebaseProvider.getInstance(this);
        FirebaseUser user = firebase.getCurrentUser();
        if (firebase.isConfigured() && user != null && resourcesRegistration == null) {
            resourcesRegistration = firebase.getFirestore().collection("users")
                    .document(user.getUid()).addSnapshotListener((snapshot, error) -> {
                        if (snapshot == null || !snapshot.exists() || error != null) return;
                        resourcesView.setText(getString(R.string.match_hud_value,
                                value(snapshot.getLong("tokens")), value(snapshot.getLong("stars")),
                                value(snapshot.getLong("league"))));
                    });
        }
    }

    @Override protected void onStop() {
        if (resourcesRegistration != null) {
            resourcesRegistration.remove();
            resourcesRegistration = null;
        }
        super.onStop();
    }

    private long value(Long number) { return number == null ? 0 : number; }

    private void leaveLobby() {
        if (currentMatch == null || currentMatch.isTerminal()) {
            finish();
            return;
        }
        FirebaseUser user = FirebaseProvider.getInstance(this).getCurrentUser();
        if (user != null && user.getUid().equals(currentMatch.abandonedByUserId)) {
            finish();
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.abandon_match_title)
                .setMessage(currentMatch.isActive()
                        ? R.string.abandon_match_message : R.string.cancel_match_message)
                .setPositiveButton(R.string.abandon_match_action,
                        (dialog, which) -> cancelOrAbandonMatch())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void cancelOrAbandonMatch() {
        if (currentInviteId == null || currentMatch == null || !currentMatch.isWaiting()) {
            viewModel.abandonMatch();
            return;
        }
        new FriendsRepository(this).cancelInvite(currentInviteId, new GameActionCallback() {
            @Override public void onSuccess() { finish(); }
            @Override public void onError(String message) { viewModel.abandonMatch(); }
        });
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
