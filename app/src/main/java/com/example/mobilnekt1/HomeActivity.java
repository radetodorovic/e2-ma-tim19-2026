package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.auth.presentation.AuthViewModel;

public class HomeActivity extends BaseKt1Activity {
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        if (!authViewModel.hasVerifiedUser()) {
            openLogin();
            return;
        }
        setContentView(R.layout.activity_home);

        Button stepByStepButton = findViewById(R.id.button_step_by_step);
        Button myNumberButton = findViewById(R.id.button_my_number);
        Button profileButton = findViewById(R.id.button_profile);
        Button quizButton = findViewById(R.id.button_quiz);
        Button connectionsButton = findViewById(R.id.button_connections);
        Button associationsButton = findViewById(R.id.button_associations);
        Button skockoButton = findViewById(R.id.button_skocko);
        Button notificationsButton = findViewById(R.id.button_notifications);
        Button changePasswordButton = findViewById(R.id.button_change_password);
        Button logoutButton = findViewById(R.id.button_logout);

        stepByStepButton.setOnClickListener(v -> startActivity(new Intent(this, StepByStepActivity.class)));
        myNumberButton.setOnClickListener(v -> startActivity(new Intent(this, MyNumberActivity.class)));
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        quizButton.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        connectionsButton.setOnClickListener(v -> startActivity(new Intent(this, ConnectionsActivity.class)));
        associationsButton.setOnClickListener(v -> startActivity(new Intent(this, AssociationsActivity.class)));
        skockoButton.setOnClickListener(v -> startActivity(new Intent(this, SkockoActivity.class)));
        notificationsButton.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        changePasswordButton.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
        logoutButton.setOnClickListener(v -> {
            authViewModel.logout();
            openLogin();
        });
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
