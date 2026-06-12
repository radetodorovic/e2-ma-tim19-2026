package com.example.mobilnekt1;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.example.mobilnekt1.games.connections.domain.ConnectionsGameState;
import com.example.mobilnekt1.games.connections.presentation.ConnectionsGameViewModel;
import java.util.Locale;

public final class MultiplayerConnectionsActivity extends BaseKt1Activity {
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private ConnectionsGameViewModel viewModel;
    private ConnectionsGameState state;
    private TextView scoreView;
    private TextView pairsView;
    private TextView headerView;
    private TextView timerView;
    private LinearLayout leftContainer;
    private LinearLayout rightContainer;
    private boolean resultShown;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections);
        String matchId = getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID);
        if (matchId == null) { finish(); return; }
        scoreView = findViewById(R.id.text_connections_score);
        pairsView = findViewById(R.id.text_connected_pairs);
        headerView = findViewById(R.id.text_connections_header);
        timerView = findViewById(R.id.text_connections_timer);
        leftContainer = findViewById(R.id.container_left_terms);
        rightContainer = findViewById(R.id.container_right_terms);
        viewModel = new ViewModelProvider(this).get(ConnectionsGameViewModel.class);
        viewModel.getState().observe(this, value -> { state = value; render(); });
        viewModel.getError().observe(this, event -> {
            String message = event.getIfNotHandled();
            if (message != null) showInfoDialog(getString(R.string.match_error_title), message);
        });
        viewModel.start(matchId);
        ticker.post(tick);
    }

    private void render() {
        if (state == null) return;
        boolean myTurn = viewModel.currentUserId() != null && viewModel.currentUserId().equals(state.activePlayerId);
        headerView.setText(getString(R.string.connections_multiplayer_header, state.round + 1,
                "steal".equals(state.phase) ? getString(R.string.remaining_pairs_phase) : getString(R.string.main_pairs_phase),
                myTurn ? getString(R.string.you) : getString(R.string.opponent)));
        scoreView.setText(getString(R.string.two_player_score, (int) state.player1Score, (int) state.player2Score));
        leftContainer.removeAllViews();
        rightContainer.removeAllViews();
        StringBuilder solvedText = new StringBuilder();
        for (int i = 0; i < state.leftItems.size(); i++) {
            TextView left = new TextView(this);
            boolean solved = state.isSolved(i);
            left.setText((i + 1) + ". " + state.leftItems.get(i));
            left.setTextColor(getResources().getColor(R.color.text_primary));
            left.setTextSize(15);
            left.setPadding(12, 12, 12, 12);
            left.setBackgroundResource(solved ? R.drawable.paired_background
                    : i == state.currentLeft ? R.drawable.selected_background : R.drawable.button_outline);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, 8);
            leftContainer.addView(left, params);
            if (solved && i < state.correctMatches.size()) {
                int right = state.correctMatches.get(i).intValue();
                if (right < state.rightItems.size()) solvedText.append(state.leftItems.get(i)).append(" - ")
                        .append(state.rightItems.get(right)).append('\n');
            }
        }
        for (int i = 0; i < state.rightItems.size(); i++) {
            Button right = new Button(this);
            right.setText(state.rightItems.get(i));
            right.setAllCaps(false);
            right.setBackgroundResource(R.drawable.button_outline);
            right.setEnabled(myTurn && !state.isFinished() && !rightSolved(i));
            int index = i;
            right.setOnClickListener(v -> { disableRightButtons(); viewModel.choose(index); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, 8);
            rightContainer.addView(right, params);
        }
        pairsView.setText(solvedText.length() == 0 ? getString(R.string.no_pairs) : solvedText.toString().trim());
        if (state.isFinished() && !resultShown) {
            resultShown = true;
            showFinishDialog(getString(R.string.game_result), getString(R.string.two_player_score,
                    (int) state.player1Score, (int) state.player2Score));
        }
    }

    private boolean rightSolved(int rightIndex) {
        for (Long left : state.solvedLeft) {
            int index = left.intValue();
            if (index < state.correctMatches.size() && state.correctMatches.get(index).intValue() == rightIndex) return true;
        }
        return false;
    }
    private void disableRightButtons() {
        for (int i = 0; i < rightContainer.getChildCount(); i++) rightContainer.getChildAt(i).setEnabled(false);
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (state != null && !state.isFinished()) {
                long remaining = Math.max(0, state.deadlineMillis - System.currentTimeMillis());
                long seconds = (long) Math.ceil(remaining / 1000.0);
                timerView.setText(String.format(Locale.getDefault(), "00:%02d", seconds));
                if (remaining == 0) viewModel.advanceExpired();
            }
            ticker.postDelayed(this, 250);
        }
    };
    @Override protected void onDestroy() { ticker.removeCallbacksAndMessages(null); super.onDestroy(); }
}
