package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends BaseKt1Activity {
    private EditText identifierInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        identifierInput = findViewById(R.id.input_identifier);
        passwordInput = findViewById(R.id.input_password);
        Button loginButton = findViewById(R.id.button_login);
        TextView registerLink = findViewById(R.id.link_register);
        TextView resetLink = findViewById(R.id.link_reset_password);

        loginButton.setOnClickListener(v -> handleLogin());
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        resetLink.setOnClickListener(v -> startActivity(new Intent(this, ResetPasswordActivity.class)));
    }

    private void handleLogin() {
        if (isBlank(identifierInput) || isBlank(passwordInput)) {
            showToast(R.string.empty_fields);
            return;
        }
        if (passwordInput.getText().toString().length() < 6) {
            showToast(R.string.password_too_short);
            return;
        }
        showToast(R.string.mock_login_success);
        startActivity(new Intent(this, HomeActivity.class));
    }
}
