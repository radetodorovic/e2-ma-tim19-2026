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
import com.example.mobilnekt1.regions.domain.RegionCatalog;

public class RegisterActivity extends BaseKt1Activity {
    private EditText emailInput;
    private EditText usernameInput;
    private EditText regionInput;
    private EditText passwordInput;
    private EditText repeatPasswordInput;
    private Button registerButton;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailInput = findViewById(R.id.input_email);
        usernameInput = findViewById(R.id.input_username);
        regionInput = findViewById(R.id.input_region);
        passwordInput = findViewById(R.id.input_password);
        repeatPasswordInput = findViewById(R.id.input_repeat_password);
        registerButton = findViewById(R.id.button_register);
        progressBar = findViewById(R.id.auth_progress);
        TextView loginLink = findViewById(R.id.link_login);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        observeViewModel();

        registerButton.setOnClickListener(v -> handleRegister());
        loginLink.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            registerButton.setEnabled(!isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        viewModel.getResult().observe(this, event -> {
            AuthResult result = event == null ? null : event.getIfNotHandled();
            if (result == null) {
                return;
            }
            if (result.status == AuthResult.Status.VERIFICATION_REQUIRED) {
                Intent intent = new Intent(this, RegistrationConfirmationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else if (result.status == AuthResult.Status.ERROR) {
                showInfoDialog(getString(R.string.registration_error_title), result.message);
            }
        });
    }

    private void handleRegister() {
        String email = emailInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String region = regionInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String repeatedPassword = repeatPasswordInput.getText().toString();
        clearErrors();

        if (!AuthValidator.isValidEmail(email)) {
            emailInput.setError(getString(R.string.invalid_email));
            return;
        }
        if (!AuthValidator.isValidUsername(username)) {
            usernameInput.setError(getString(R.string.invalid_username));
            return;
        }
        if (region.isEmpty()) {
            regionInput.setError(getString(R.string.required_field));
            return;
        }
        if (!RegionCatalog.isSupported(region)) {
            regionInput.setError(getString(R.string.supported_regions));
            return;
        }
        if (!AuthValidator.isStrongPassword(password)) {
            passwordInput.setError(getString(R.string.weak_password));
            return;
        }
        if (!password.equals(repeatedPassword)) {
            repeatPasswordInput.setError(getString(R.string.passwords_do_not_match));
            return;
        }
        viewModel.register(email, username, region, password);
    }

    private void clearErrors() {
        emailInput.setError(null);
        usernameInput.setError(null);
        regionInput.setError(null);
        passwordInput.setError(null);
        repeatPasswordInput.setError(null);
    }
}
