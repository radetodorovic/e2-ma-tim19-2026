package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class ProfileActivity extends BaseKt1Activity {
    private TextView avatarView;
    private int selectedAvatar = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        avatarView = findViewById(R.id.text_avatar);
        TextView usernameView = findViewById(R.id.text_profile_username);
        TextView emailView = findViewById(R.id.text_profile_email);
        TextView tokensView = findViewById(R.id.text_profile_tokens);
        TextView starsView = findViewById(R.id.text_profile_stars);
        TextView leagueView = findViewById(R.id.text_profile_league);
        TextView regionView = findViewById(R.id.text_profile_region);
        Button changeAvatarButton = findViewById(R.id.button_change_avatar);
        Button statisticsButton = findViewById(R.id.button_open_statistics);
        Button logoutButton = findViewById(R.id.button_profile_logout);

        usernameView.setText(MockStudentTwoData.USERNAME);
        emailView.setText(MockStudentTwoData.EMAIL);
        tokensView.setText("Tokeni: " + MockStudentTwoData.TOKENS);
        starsView.setText("Zvezde: " + MockStudentTwoData.STARS);
        leagueView.setText("Liga: " + MockStudentTwoData.LEAGUE);
        leagueView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_silver_medal, 0, 0, 0);
        leagueView.setCompoundDrawablePadding(8);
        regionView.setText("Region: " + MockStudentTwoData.REGION);
        renderAvatar();

        changeAvatarButton.setOnClickListener(v -> showAvatarDialog());
        statisticsButton.setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    private void showAvatarDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.change_avatar)
                .setItems(MockStudentTwoData.AVATARS, (dialog, which) -> {
                    selectedAvatar = which;
                    renderAvatar();
                    showToast(R.string.avatar_changed_mock);
                })
                .show();
    }

    private void renderAvatar() {
        avatarView.setText(MockStudentTwoData.AVATARS[selectedAvatar]);
    }
}
