package com.example.mobilnekt1;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.auth.presentation.AuthViewModel;
import com.example.mobilnekt1.notifications.presentation.NotificationChannels;
import com.example.mobilnekt1.notifications.data.PushTokenRepository;
import com.example.mobilnekt1.notifications.data.ChatNotificationObserver;
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
        boolean guest = authViewModel.isGuest();
        if (!guest) {
            requestNotificationPermission();
            new PushTokenRepository(this).refresh();
            ChatNotificationObserver.start(this);
        }
        userRepository = new UserRepository(this);
        userRepository.ensureCurrentUserDefaults(true, new GameActionCallback() {
            @Override public void onSuccess() { if (!guest) claimDailyTokens(); }
            @Override public void onError(String message) { }
        });

        Button stepByStepButton = findViewById(R.id.button_step_by_step);
        Button tournamentButton = findViewById(R.id.button_tournament);
        Button matchLobbyButton = findViewById(R.id.button_match_lobby);
        Button myNumberButton = findViewById(R.id.button_my_number);
        Button profileButton = findViewById(R.id.button_profile);
        Button quizButton = findViewById(R.id.button_quiz);
        Button connectionsButton = findViewById(R.id.button_connections);
        Button associationsButton = findViewById(R.id.button_associations);
        Button skockoButton = findViewById(R.id.button_skocko);
        Button notificationsButton = findViewById(R.id.button_notifications);
        Button rankingButton = findViewById(R.id.button_ranking);
        Button regionsButton = findViewById(R.id.button_regions);
        Button chatButton = findViewById(R.id.button_chat);
        Button missionsButton = findViewById(R.id.button_daily_missions);
        Button friendsButton = findViewById(R.id.button_friends);
        Button changePasswordButton = findViewById(R.id.button_change_password);
        Button logoutButton = findViewById(R.id.button_logout);

        if (guest) {
            int[] registeredOnly = { R.id.button_step_by_step, R.id.button_my_number,
                    R.id.button_tournament,
                    R.id.button_profile, R.id.button_quiz, R.id.button_connections,
                    R.id.button_associations, R.id.button_skocko, R.id.button_notifications,
                    R.id.button_ranking, R.id.button_regions, R.id.button_chat,
                    R.id.button_daily_missions, R.id.button_friends, R.id.button_change_password };
            for (int id : registeredOnly) findViewById(id).setVisibility(android.view.View.GONE);
        }

        stepByStepButton.setOnClickListener(v ->
                startActivity(new Intent(this, MatchLobbyActivity.class)));
        tournamentButton.setOnClickListener(v -> startActivity(new Intent(this, TournamentActivity.class)));
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
        rankingButton.setOnClickListener(v -> startActivity(new Intent(this, RankingActivity.class)));
        regionsButton.setOnClickListener(v -> startActivity(new Intent(this, RegionsActivity.class)));
        chatButton.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        missionsButton.setOnClickListener(v -> startActivity(new Intent(this, DailyMissionsActivity.class)));
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

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
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
