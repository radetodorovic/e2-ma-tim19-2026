package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.auth.presentation.AuthViewModel;
import com.example.mobilnekt1.notifications.presentation.NotificationChannels;
import com.example.mobilnekt1.profile.data.DailyTokenCallback;
import com.example.mobilnekt1.profile.data.UserRepository;
import com.example.mobilnekt1.profile.data.UserProfileCallback;
import com.example.mobilnekt1.profile.domain.UserProfile;
import com.example.mobilnekt1.games.shared.GameActionCallback;

public class HomeActivity extends BaseKt1Activity {
    private AuthViewModel authViewModel;
    private UserRepository userRepository;
    private boolean activeMatchOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        if (!authViewModel.hasVerifiedUser()) {
            openLogin();
            return;
        }
        setContentView(R.layout.activity_home);
        NotificationChannels.create(this);
        userRepository = new UserRepository(this);
        userRepository.ensureCurrentUserDefaults(true, new GameActionCallback() {
            @Override public void onSuccess() { claimDailyTokens(); }
            @Override public void onError(String message) { }
        });

        Button stepByStepButton = findViewById(R.id.button_step_by_step);
        Button matchLobbyButton = findViewById(R.id.button_match_lobby);
        Button myNumberButton = findViewById(R.id.button_my_number);
        Button profileButton = findViewById(R.id.button_profile);
        Button quizButton = findViewById(R.id.button_quiz);
        Button connectionsButton = findViewById(R.id.button_connections);
        Button associationsButton = findViewById(R.id.button_associations);
        Button skockoButton = findViewById(R.id.button_skocko);
        Button notificationsButton = findViewById(R.id.button_notifications);
        Button friendsButton = findViewById(R.id.button_friends);
        Button changePasswordButton = findViewById(R.id.button_change_password);
        Button logoutButton = findViewById(R.id.button_logout);

        stepByStepButton.setOnClickListener(v ->
                startActivity(new Intent(this, MatchLobbyActivity.class)));
        matchLobbyButton.setOnClickListener(v ->
                startActivity(new Intent(this, MatchLobbyActivity.class)));
        myNumberButton.setOnClickListener(v ->
                startActivity(new Intent(this, MatchLobbyActivity.class)));
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        quizButton.setOnClickListener(v -> startActivity(new Intent(this, MatchLobbyActivity.class)));
        connectionsButton.setOnClickListener(v -> startActivity(new Intent(this, MatchLobbyActivity.class)));
        associationsButton.setOnClickListener(v -> startActivity(new Intent(this, MatchLobbyActivity.class)));
        skockoButton.setOnClickListener(v -> startActivity(new Intent(this, MatchLobbyActivity.class)));
        notificationsButton.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        friendsButton.setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        changePasswordButton.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
        logoutButton.setOnClickListener(v -> {
            authViewModel.logout();
            openLogin();
        });
    }

    private void claimDailyTokens() {
        userRepository.claimDailyTokens(new DailyTokenCallback() {
            @Override public void onComplete(boolean claimed, long amount) {
                if (claimed) {
                    Toast.makeText(HomeActivity.this,
                            getString(R.string.daily_tokens_received, amount), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onError(String message) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeMatchOpened = false;
        if (userRepository == null) return;
        userRepository.getCurrentUser(new UserProfileCallback() {
            @Override public void onSuccess(UserProfile profile) {
                if (!profile.inGame || profile.activeMatchId == null || activeMatchOpened) return;
                activeMatchOpened = true;
                Intent intent = new Intent(HomeActivity.this, MatchLobbyActivity.class);
                intent.putExtra(MatchLobbyActivity.EXTRA_MATCH_ID, profile.activeMatchId);
                startActivity(intent);
            }

            @Override public void onError(String message) { }
        });
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
