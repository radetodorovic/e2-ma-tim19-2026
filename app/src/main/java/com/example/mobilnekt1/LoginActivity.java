package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.auth.domain.AuthValidator;
import com.example.mobilnekt1.auth.presentation.AuthResult;
import com.example.mobilnekt1.auth.presentation.AuthViewModel;
import com.example.mobilnekt1.auth.presentation.Event;

public class LoginActivity extends BaseKt1Activity {
    private EditText identifierInput;
    private EditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        identifierInput = findViewById(R.id.input_identifier);
        passwordInput = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.button_login);
        progressBar = findViewById(R.id.auth_progress);
        TextView registerLink = findViewById(R.id.link_register);
        TextView resetLink = findViewById(R.id.link_reset_password);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        observeViewModel();

        loginButton.setOnClickListener(v -> handleLogin());
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        resetLink.setOnClickListener(v -> startActivity(new Intent(this, ResetPasswordActivity.class)));

        if (viewModel.hasVerifiedUser()) {
            openHome();
        } else if (viewModel.hasUserAwaitingVerification()) {
            openConfirmation();
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            loginButton.setEnabled(!isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        viewModel.getResult().observe(this, event -> {
            AuthResult result = getResult(event);
            if (result == null) {
                return;
            }
            if (result.status == AuthResult.Status.SUCCESS) {
                openHome();
            } else if (result.status == AuthResult.Status.VERIFICATION_REQUIRED) {
                openConfirmation();
            } else {
                showInfoDialog(getString(R.string.login_error_title), result.message);
            }
        });
    }

    private void handleLogin() {
        String identifier = identifierInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        identifierInput.setError(null);
        passwordInput.setError(null);

        if (identifier.isEmpty()) {
            identifierInput.setError(getString(R.string.required_field));
            return;
        }
        if (!identifier.contains("@") && !AuthValidator.isValidUsername(identifier)) {
            identifierInput.setError(getString(R.string.invalid_username));
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.required_field));
            return;
        }
        viewModel.login(identifier, password);
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void openConfirmation() {
        startActivity(new Intent(this, RegistrationConfirmationActivity.class));
    }

    private AuthResult getResult(Event<AuthResult> event) {
        return event == null ? null : event.getIfNotHandled();
    }
}
