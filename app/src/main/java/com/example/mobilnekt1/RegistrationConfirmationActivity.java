package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class RegistrationConfirmationActivity extends BaseKt1Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_confirmation);

        Button loginButton = findViewById(R.id.button_back_to_login);
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}
