package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.auth.presentation.AuthResult;
import com.example.mobilnekt1.auth.presentation.AuthViewModel;

public class RegistrationConfirmationActivity extends BaseKt1Activity {
    private AuthViewModel viewModel;
    private Button checkButton;
    private Button resendButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_confirmation);

        checkButton = findViewById(R.id.button_check_verification);
        resendButton = findViewById(R.id.button_resend_verification);
        progressBar = findViewById(R.id.auth_progress);
        Button loginButton = findViewById(R.id.button_back_to_login);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        observeViewModel();

        checkButton.setOnClickListener(v -> viewModel.checkEmailVerification());
        resendButton.setOnClickListener(v -> viewModel.resendVerification());
        loginButton.setOnClickListener(v -> {
            viewModel.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            checkButton.setEnabled(!isLoading);
            resendButton.setEnabled(!isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        viewModel.getResult().observe(this, event -> {
            AuthResult result = event == null ? null : event.getIfNotHandled();
            if (result == null) {
                return;
            }
            if (result.status == AuthResult.Status.SUCCESS) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else if (result.status == AuthResult.Status.VERIFICATION_REQUIRED) {
                showToast(R.string.verification_pending_or_sent);
            } else {
                showInfoDialog(getString(R.string.verification_error_title), result.message);
            }
        });
    }
}
