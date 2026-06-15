package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.games.skocko.data.SkockoGameListener;
import com.example.mobilnekt1.games.skocko.data.SkockoGameRepository;
import com.example.mobilnekt1.games.skocko.domain.SkockoGameState;
import com.example.mobilnekt1.match.data.MatchScoreRepository;
import com.example.mobilnekt1.profile.data.StatsRepository;

import java.util.Locale;

public final class MultiplayerSkockoActivity extends BaseKt1Activity {
    private static final String[] SYMBOLS = {"Skocko", "Kvadrat", "Krug", "Srce", "Trougao", "Zvezda"};
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final int[] guess = new int[4];
    private int slot;
    private SkockoGameRepository repository;
    private MatchScoreRepository matchScoreRepository;
    private StatsRepository statsRepository;
    private SkockoGameState state;
    private String matchId;
    private TextView statusView;
    private TextView guessView;
    private LinearLayout historyContainer;
    private Button confirmButton;
    private boolean advancing;
    private boolean committed;
    private boolean resultShown;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skocko);
        matchId = getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID);
        if (matchId == null) { finish(); return; }
        statusView = findViewById(R.id.text_skocko_status);
        guessView = findViewById(R.id.text_skocko_guess);
        historyContainer = findViewById(R.id.container_skocko_history);
        confirmButton = findViewById(R.id.button_confirm_skocko);
        buildSymbols(findViewById(R.id.container_skocko_symbols));
        findViewById(R.id.button_clear_skocko).setOnClickListener(v -> { slot = 0; renderGuess(); });
        confirmButton.setOnClickListener(v -> submit());
        repository = new SkockoGameRepository(this);
        matchScoreRepository = new MatchScoreRepository(this);
        statsRepository = new StatsRepository(this);
        repository.listen(matchId, new SkockoGameListener() {
            @Override public void onChanged(SkockoGameState value) {
                state = value; advancing = false; slot = 0; render();
                if (value.isFinished() && !committed) {
                    committed = true;
                    statsRepository.commitSkocko(matchId, callback());
                    matchScoreRepository.commitGameResult(matchId, "skocko", "phase", callback());
                }
            }
            @Override public void onError(String message) { showInfoDialog(getString(R.string.match_error_title), message); }
        });
        repository.initialize(matchId, callback());
        ticker.post(tick);
    }

    private void buildSymbols(GridLayout container) {
        for (int i = 0; i < SYMBOLS.length; i++) {
            Button button = new Button(this); button.setText(SYMBOLS[i]); button.setAllCaps(false);
            button.setBackgroundResource(R.drawable.button_outline);
            int symbol = i; button.setOnClickListener(v -> { if (slot < 4) { guess[slot++] = symbol; renderGuess(); } });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            container.addView(button, params);
        }
    }
    private void submit() {
        if (slot != 4) { showToast(R.string.choose_four_symbols); return; }
        confirmButton.setEnabled(false);
        repository.submitGuess(matchId, guess.clone(), callback());
    }
    private void render() {
        if (state == null) return;
        boolean myTurn = repository.currentUserId() != null && repository.currentUserId().equals(state.activePlayerId);
        long seconds = (long) Math.ceil(Math.max(0, state.deadlineMillis - System.currentTimeMillis()) / 1000.0);
        statusView.setText(String.format(Locale.getDefault(), "Runda %d/2 | %s | %s | 00:%02d | Pokusaj %d/6",
                state.round + 1, "steal".equals(state.phase) ? "protivnicka sansa" : "glavna igra",
                myTurn ? "vas potez" : "protivnikov potez", seconds, state.attempt + 1));
        confirmButton.setEnabled(myTurn && !state.isFinished() && slot == 4);
        renderHistory(); renderGuess();
        if (state.isFinished() && !resultShown) {
            resultShown = true;
            showFinishDialog(getString(R.string.game_result), getString(R.string.two_player_score,
                    (int) state.player1Score, (int) state.player2Score));
        }
    }
    private void renderHistory() {
        historyContainer.removeAllViews();
        for (String encoded : state.history) {
            String[] pieces = encoded.split("\\|");
            String[] values = pieces[0].split(",");
            StringBuilder text = new StringBuilder();
            for (String value : values) { if (text.length() > 0) text.append(" - "); text.append(SYMBOLS[Integer.parseInt(value)]); }
            if (pieces.length >= 3) text.append(" | Tacno: ").append(pieces[1]).append(", pogresno mesto: ").append(pieces[2]);
            TextView row = new TextView(this); row.setText(text); row.setTextColor(getResources().getColor(R.color.text_primary));
            row.setPadding(0, 6, 0, 6); historyContainer.addView(row);
        }
    }
    private void renderGuess() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < slot; i++) { if (i > 0) text.append(" - "); text.append(SYMBOLS[guess[i]]); }
        guessView.setText("Trenutna kombinacija: " + (text.length() == 0 ? "----" : text));
        if (state != null) confirmButton.setEnabled(repository.currentUserId() != null
                && repository.currentUserId().equals(state.activePlayerId) && !state.isFinished() && slot == 4);
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
        @Override public void onError(String message) { advancing = false; render(); showInfoDialog(getString(R.string.match_error_title), message); }
    }; }
    @Override protected void onDestroy() { ticker.removeCallbacksAndMessages(null); if (repository != null) repository.stopListening(); super.onDestroy(); }
}
