package com.example.mobilnekt1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MyNumberActivity extends BaseKt1Activity {
    private TextView targetView;
    private TextView numbersView;
    private TextView expressionView;
    private TextView pointsView;
    private String expression = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_number);

        targetView = findViewById(R.id.text_target_number);
        numbersView = findViewById(R.id.text_offered_numbers);
        expressionView = findViewById(R.id.text_expression);
        pointsView = findViewById(R.id.text_my_number_points);

        findViewById(R.id.button_stop_target).setOnClickListener(v -> targetView.setText(String.valueOf(MockGameData.MY_NUMBER_TARGET)));
        findViewById(R.id.button_stop_numbers).setOnClickListener(v -> numbersView.setText(formatNumbers()));
        findViewById(R.id.button_clear_expression).setOnClickListener(v -> {
            expression = "";
            renderExpression();
        });
        findViewById(R.id.button_confirm_expression).setOnClickListener(v -> showResult());

        int[] valueButtons = {
                R.id.button_num_1, R.id.button_num_2, R.id.button_num_3,
                R.id.button_num_4, R.id.button_num_5, R.id.button_num_6
        };
        for (int i = 0; i < valueButtons.length; i++) {
            int value = MockGameData.MY_NUMBER_VALUES[i];
            Button button = findViewById(valueButtons[i]);
            button.setText(String.valueOf(value));
            button.setOnClickListener(v -> appendToken(String.valueOf(value)));
        }

        int[] operatorButtons = {
                R.id.button_op_open, R.id.button_op_close, R.id.button_op_plus,
                R.id.button_op_minus, R.id.button_op_multiply, R.id.button_op_divide
        };
        String[] operators = {"(", ")", "+", "-", "*", "/"};
        for (int i = 0; i < operatorButtons.length; i++) {
            String operator = operators[i];
            Button button = findViewById(operatorButtons[i]);
            button.setText(operator);
            button.setOnClickListener(v -> appendToken(operator));
        }
    }

    private String formatNumbers() {
        StringBuilder builder = new StringBuilder();
        for (int value : MockGameData.MY_NUMBER_VALUES) {
            if (builder.length() > 0) {
                builder.append("  ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private void appendToken(String token) {
        expression = expression + token + " ";
        renderExpression();
    }

    private void renderExpression() {
        expressionView.setText(expression.isEmpty() ? "Izraz jos nije unet." : expression);
    }

    private void showResult() {
        if (expression.trim().isEmpty()) {
            showToast(R.string.empty_fields);
            return;
        }
        pointsView.setText("Mock bodovi: 10");
        showFinishDialog(getString(R.string.round_result),
                "Izraz: " + expression
                        + "\nParser izraza nije implementiran za KT1."
                        + "\nPrikazan je mock rezultat runde i 10 bodova za tacan broj.");
    }
}
