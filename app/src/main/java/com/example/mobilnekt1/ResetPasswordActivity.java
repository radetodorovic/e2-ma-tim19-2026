package com.example.mobilnekt1;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.mobilnekt1.auth.domain.AuthValidator;
import com.example.mobilnekt1.auth.presentation.AuthResult;
import com.example.mobilnekt1.auth.presentation.AuthViewModel;

public class ResetPasswordActivity extends BaseKt1Activity {
    private EditText emailInput;
    private EditText oldPasswordInput;
    private EditText newPasswordInput;
    private EditText repeatNewPasswordInput;
    private LinearLayout changePasswordFields;
    private Button confirmButton;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;
    private boolean changeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        emailInput = findViewById(R.id.input_reset_email);
        oldPasswordInput = findViewById(R.id.input_old_password);
        newPasswordInput = findViewById(R.id.input_new_password);
        repeatNewPasswordInput = findViewById(R.id.input_repeat_new_password);
        changePasswordFields = findViewById(R.id.change_password_fields);
        confirmButton = findViewById(R.id.button_confirm_reset);
        progressBar = findViewById(R.id.auth_progress);
        TextView description = findViewById(R.id.password_screen_description);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        changeMode = viewModel.hasVerifiedUser();
        emailInput.setVisibility(changeMode ? View.GONE : View.VISIBLE);
        changePasswordFields.setVisibility(changeMode ? View.VISIBLE : View.GONE);
        description.setText(changeMode ? R.string.change_password_description
                : R.string.forgot_password_description);
        confirmButton.setText(changeMode ? R.string.change_password : R.string.send_reset_link);

        observeViewModel();
        confirmButton.setOnClickListener(v -> handleSubmit());
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            confirmButton.setEnabled(!isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        viewModel.getResult().observe(this, event -> {
            AuthResult result = event == null ? null : event.getIfNotHandled();
            if (result == null) {
                return;
            }
            if (result.status == AuthResult.Status.SUCCESS) {
                showFinishDialog(getString(R.string.reset_password_title),
                        getString(changeMode ? R.string.password_changed : R.string.reset_email_sent));
            } else if (result.status == AuthResult.Status.ERROR) {
                showInfoDialog(getString(R.string.password_error_title), result.message);
            }
        });
    }

    private void handleSubmit() {
        if (!changeMode) {
            String email = emailInput.getText().toString().trim();
            if (!AuthValidator.isValidEmail(email)) {
                emailInput.setError(getString(R.string.invalid_email));
                return;
            }
            viewModel.sendPasswordReset(email);
            return;
        }

        String oldPassword = oldPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String repeatedPassword = repeatNewPasswordInput.getText().toString();
        if (oldPassword.isEmpty()) {
            oldPasswordInput.setError(getString(R.string.required_field));
            return;
        }
        if (!AuthValidator.isStrongPassword(newPassword)) {
            newPasswordInput.setError(getString(R.string.weak_password));
            return;
        }
        if (!newPassword.equals(repeatedPassword)) {
            repeatNewPasswordInput.setError(getString(R.string.passwords_do_not_match));
            return;
        }
        viewModel.changePassword(oldPassword, newPassword);
    }
}
