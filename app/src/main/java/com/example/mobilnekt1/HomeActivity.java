package com.example.mobilnekt1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class HomeActivity extends BaseKt1Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button stepByStepButton = findViewById(R.id.button_step_by_step);
        Button myNumberButton = findViewById(R.id.button_my_number);
        Button logoutButton = findViewById(R.id.button_logout);

        stepByStepButton.setOnClickListener(v -> startActivity(new Intent(this, StepByStepActivity.class)));
        myNumberButton.setOnClickListener(v -> startActivity(new Intent(this, MyNumberActivity.class)));
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}
