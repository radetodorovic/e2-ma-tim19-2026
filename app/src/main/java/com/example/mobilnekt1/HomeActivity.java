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
        Button profileButton = findViewById(R.id.button_profile);
        Button quizButton = findViewById(R.id.button_quiz);
        Button connectionsButton = findViewById(R.id.button_connections);

        stepByStepButton.setOnClickListener(v -> startActivity(new Intent(this, StepByStepActivity.class)));
        myNumberButton.setOnClickListener(v -> startActivity(new Intent(this, MyNumberActivity.class)));
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        quizButton.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        connectionsButton.setOnClickListener(v -> startActivity(new Intent(this, ConnectionsActivity.class)));
    }
}
