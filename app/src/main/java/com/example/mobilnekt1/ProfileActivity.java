package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.example.mobilnekt1.profile.domain.UserProfile;
import com.example.mobilnekt1.profile.presentation.ProfileViewModel;

public class ProfileActivity extends BaseKt1Activity {
    private static final String[] AVATARS = {"M1", "M2", "M3", "M4"};
    private ProfileViewModel viewModel;
    private TextView avatarView, usernameView, emailView, tokensView, starsView, leagueView, regionView, qrView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        avatarView = findViewById(R.id.text_avatar);
        usernameView = findViewById(R.id.text_profile_username);
        emailView = findViewById(R.id.text_profile_email);
        tokensView = findViewById(R.id.text_profile_tokens);
        starsView = findViewById(R.id.text_profile_stars);
        leagueView = findViewById(R.id.text_profile_league);
        regionView = findViewById(R.id.text_profile_region);
        qrView = findViewById(R.id.text_profile_qr);
        Button changeAvatarButton = findViewById(R.id.button_change_avatar);
        Button statisticsButton = findViewById(R.id.button_open_statistics);
        Button logoutButton = findViewById(R.id.button_profile_logout);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getState().observe(this, state -> render(state.profile));
        viewModel.getError().observe(this, event -> {
            String message = event.getIfNotHandled();
            if (message != null) showInfoDialog(getString(R.string.profile_error_title), message);
        });
        changeAvatarButton.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.change_avatar).setItems(AVATARS,
                        (dialog, which) -> viewModel.updateAvatar(AVATARS[which])).show());
        statisticsButton.setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));
        logoutButton.setOnClickListener(v -> {
            viewModel.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void render(UserProfile profile) {
        avatarView.setText(profile.avatarId);
        usernameView.setText(profile.username);
        emailView.setText(profile.email);
        tokensView.setText(getString(R.string.profile_tokens_value, profile.tokens));
        starsView.setText(getString(R.string.profile_stars_value, profile.stars));
        leagueView.setText(getString(R.string.profile_league_value, leagueName(profile.league)));
        leagueView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_silver_medal, 0, 0, 0);
        regionView.setText(getString(R.string.profile_region_value, profile.region));
        qrView.setText(profile.qrCodeValue);
    }

    private String leagueName(long league) {
        String[] names = {"Pocetna", "Bronzana", "Srebrna", "Zlatna", "Platinasta", "Dijamantska"};
        return names[(int) Math.max(0, Math.min(league, names.length - 1))];
    }
}
