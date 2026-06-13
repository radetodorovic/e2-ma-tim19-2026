package com.example.mobilnekt1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.example.mobilnekt1.profile.domain.UserProfile;
import com.example.mobilnekt1.profile.presentation.ProfileViewModel;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class ProfileActivity extends BaseKt1Activity {
    private static final String[] AVATAR_IDS = {"M1", "M2", "M3"};
    private static final int[] AVATAR_RESOURCES = {
            R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3
    };
    private ProfileViewModel viewModel;
    private ImageView avatarView, qrView;
    private TextView usernameView, emailView, tokensView, starsView, leagueView, regionView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        avatarView = findViewById(R.id.image_avatar);
        usernameView = findViewById(R.id.text_profile_username);
        emailView = findViewById(R.id.text_profile_email);
        tokensView = findViewById(R.id.text_profile_tokens);
        starsView = findViewById(R.id.text_profile_stars);
        leagueView = findViewById(R.id.text_profile_league);
        regionView = findViewById(R.id.text_profile_region);
        qrView = findViewById(R.id.image_profile_qr);
        Button changeAvatarButton = findViewById(R.id.button_change_avatar);
        Button statisticsButton = findViewById(R.id.button_open_statistics);
        Button logoutButton = findViewById(R.id.button_profile_logout);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.getState().observe(this, state -> render(state.profile));
        viewModel.getError().observe(this, event -> {
            String message = event.getIfNotHandled();
            if (message != null) showInfoDialog(getString(R.string.profile_error_title), message);
        });
        changeAvatarButton.setOnClickListener(v -> showAvatarPicker());
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
        avatarView.setImageResource(avatarResource(profile.avatarId));
        usernameView.setText(profile.username);
        emailView.setText(profile.email);
        tokensView.setText(getString(R.string.profile_tokens_value, profile.tokens));
        starsView.setText(getString(R.string.profile_stars_value, profile.stars));
        leagueView.setText(getString(R.string.profile_league_value, leagueName(profile.league)));
        leagueView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_silver_medal, 0, 0, 0);
        regionView.setText(getString(R.string.profile_region_value, profile.region));
        String qrValue = profile.qrCodeValue;
        if (qrValue == null || qrValue.trim().isEmpty()) {
            qrValue = "slagalica:user:" + profile.uid;
        }
        renderQrCode(qrValue);
    }

    private void showAvatarPicker() {
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        int padding = dp(12);
        choices.setPadding(padding, padding, padding, padding);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.change_avatar)
                .setView(choices)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        for (int i = 0; i < AVATAR_RESOURCES.length; i++) {
            final int index = i;
            ImageButton button = new ImageButton(this);
            button.setImageResource(AVATAR_RESOURCES[i]);
            button.setScaleType(ImageView.ScaleType.CENTER_CROP);
            button.setContentDescription(getString(R.string.profile_avatar));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(96), 1f);
            params.setMargins(dp(4), 0, dp(4), 0);
            choices.addView(button, params);
            button.setOnClickListener(v -> {
                viewModel.updateAvatar(AVATAR_IDS[index]);
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    private int avatarResource(String avatarId) {
        for (int i = 0; i < AVATAR_IDS.length; i++) {
            if (AVATAR_IDS[i].equals(avatarId)) {
                return AVATAR_RESOURCES[i];
            }
        }
        return AVATAR_RESOURCES[0];
    }

    private void renderQrCode(String value) {
        try {
            int size = dp(220);
            BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qrView.setImageBitmap(bitmap);
        } catch (WriterException error) {
            qrView.setImageDrawable(null);
            showInfoDialog(getString(R.string.profile_error_title),
                    getString(R.string.qr_generation_error));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String leagueName(long league) {
        String[] names = {"Pocetna", "Bronzana", "Srebrna", "Zlatna", "Platinasta", "Dijamantska"};
        return names[(int) Math.max(0, Math.min(league, names.length - 1))];
    }
}
