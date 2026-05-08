package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RegisterActivity extends BaseKt1Activity {
    private EditText emailInput;
    private EditText usernameInput;
    private EditText regionInput;
    private EditText passwordInput;
    private EditText repeatPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailInput = findViewById(R.id.input_email);
        usernameInput = findViewById(R.id.input_username);
        regionInput = findViewById(R.id.input_region);
        passwordInput = findViewById(R.id.input_password);
        repeatPasswordInput = findViewById(R.id.input_repeat_password);
        Button registerButton = findViewById(R.id.button_register);
        TextView loginLink = findViewById(R.id.link_login);

        registerButton.setOnClickListener(v -> handleRegister());
        loginLink.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        if (isBlank(emailInput) || isBlank(usernameInput) || isBlank(regionInput)
                || isBlank(passwordInput) || isBlank(repeatPasswordInput)) {
            showToast(R.string.empty_fields);
            return;
        }
        String password = passwordInput.getText().toString();
        if (password.length() < 6) {
            showToast(R.string.password_too_short);
            return;
        }
        if (!password.equals(repeatPasswordInput.getText().toString())) {
            showToast(R.string.passwords_do_not_match);
            return;
        }
        startActivity(new Intent(this, RegistrationConfirmationActivity.class));
    }
}
