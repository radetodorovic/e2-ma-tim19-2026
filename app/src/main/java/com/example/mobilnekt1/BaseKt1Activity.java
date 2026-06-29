package com.example.mobilnekt1;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilnekt1.core.data.FirebaseProvider;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.match.data.MatchCompletionRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

public abstract class BaseKt1Activity extends AppCompatActivity {
    private TextView matchHud;
    private ListenerRegistration hudRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String matchId = getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID);
        if (matchId != null && !(this instanceof MatchLobbyActivity)) {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override public void handleOnBackPressed() { confirmAbandonMatch(matchId); }
            });
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        if (getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID) != null
                && !(this instanceof MatchLobbyActivity)) {
            attachMatchHud();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startHudListener();
    }

    @Override
    protected void onStop() {
        if (hudRegistration != null) {
            hudRegistration.remove();
            hudRegistration = null;
        }
        super.onStop();
    }

    protected boolean isBlank(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    protected void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    protected void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    protected void showFinishDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void attachMatchHud() {
        View content = findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup) || ((ViewGroup) content).getChildCount() == 0) return;
        View root = ((ViewGroup) content).getChildAt(0);
        int hudHeight = dp(44);
        root.setPadding(root.getPaddingLeft(), root.getPaddingTop() + hudHeight,
                root.getPaddingRight(), root.getPaddingBottom());
        matchHud = new TextView(this);
        matchHud.setText(R.string.match_hud_loading);
        matchHud.setTextColor(Color.WHITE);
        matchHud.setTextSize(15);
        matchHud.setGravity(Gravity.CENTER);
        matchHud.setBackgroundColor(getResources().getColor(R.color.primary));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, hudHeight, Gravity.TOP);
        addContentView(matchHud, params);
    }

    private void startHudListener() {
        if (matchHud == null || hudRegistration != null) return;
        FirebaseProvider firebase = FirebaseProvider.getInstance(this);
        FirebaseUser user = firebase.getCurrentUser();
        if (!firebase.isConfigured() || user == null) return;
        hudRegistration = firebase.getFirestore().collection("users").document(user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null || !snapshot.exists() || error != null) return;
                    matchHud.setText(getString(R.string.match_hud_value,
                            number(snapshot.getLong("tokens")),
                            number(snapshot.getLong("stars")),
                            number(snapshot.getLong("league"))));
                });
    }

    private void confirmAbandonMatch(String matchId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.abandon_match_title)
                .setMessage(R.string.abandon_match_message)
                .setPositiveButton(R.string.abandon_match_action, (dialog, which) ->
                        new MatchCompletionRepository(this).abandonMatch(matchId,
                                new GameActionCallback() {
                                    @Override public void onSuccess() { finish(); }
                                    @Override public void onError(String message) {
                                        showInfoDialog(getString(R.string.match_error_title), message);
                                    }
                                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private long number(Long value) {
        return value == null ? 0 : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
