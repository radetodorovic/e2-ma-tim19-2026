package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class ResetPasswordActivity extends BaseKt1Activity {
    private EditText oldPasswordInput;
    private EditText newPasswordInput;
    private EditText repeatNewPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        oldPasswordInput = findViewById(R.id.input_old_password);
        newPasswordInput = findViewById(R.id.input_new_password);
        repeatNewPasswordInput = findViewById(R.id.input_repeat_new_password);
        Button confirmButton = findViewById(R.id.button_confirm_reset);

        confirmButton.setOnClickListener(v -> handleReset());
    }

    private void handleReset() {
        if (isBlank(oldPasswordInput) || isBlank(newPasswordInput) || isBlank(repeatNewPasswordInput)) {
            showToast(R.string.empty_fields);
            return;
        }
        String newPassword = newPasswordInput.getText().toString();
        if (newPassword.length() < 6) {
            showToast(R.string.password_too_short);
            return;
        }
        if (!newPassword.equals(repeatNewPasswordInput.getText().toString())) {
            showToast(R.string.passwords_do_not_match);
            return;
        }
        showInfoDialog(getString(R.string.reset_password_title), getString(R.string.mock_reset_success));
    }
}
