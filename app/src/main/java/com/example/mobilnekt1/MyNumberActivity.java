package com.example.mobilnekt1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.mobilnekt1.games.mynumber.ExpressionEvaluator;
import com.example.mobilnekt1.games.mynumber.MyNumberGenerator;
import com.example.mobilnekt1.games.mynumber.MyNumberRound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MyNumberActivity extends BaseKt1Activity implements SensorEventListener {
    private enum Stage {TARGET_ROLLING, NUMBERS_ROLLING, PLAYING, FINISHED}

    private final Handler rollingHandler = new Handler(Looper.getMainLooper());
    private final Random displayRandom = new Random();
    private final MyNumberGenerator generator = new MyNumberGenerator();
    private final List<String> expressionTokens = new ArrayList<>();

    private TextView headerView;
    private TextView timerView;
    private TextView targetView;
    private TextView numbersView;
    private TextView expressionView;
    private TextView pointsView;
    private TextView shakeView;
    private Button stopTargetButton;
    private Button stopNumbersButton;
    private Button confirmButton;
    private Button[] numberButtons;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private CountDownTimer stageTimer;
    private CountDownTimer playTimer;
    private MyNumberRound gameRound;
    private Stage stage = Stage.TARGET_ROLLING;
    private int roundIndex;
    private int currentPlayer = 1;
    private int playerOneScore;
    private int playerTwoScore;
    private Integer playerOneResult;
    private Integer playerTwoResult;
    private long remainingMillis;
    private long lastShakeTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_number);

        bindViews();
        configureSensors();
        configureButtons();
        startNewRound();
    }

    private void bindViews() {
        headerView = findViewById(R.id.text_my_number_header);
        timerView = findViewById(R.id.text_my_number_timer);
        targetView = findViewById(R.id.text_target_number);
        numbersView = findViewById(R.id.text_offered_numbers);
        expressionView = findViewById(R.id.text_expression);
        pointsView = findViewById(R.id.text_my_number_points);
        shakeView = findViewById(R.id.text_shake_status);
        stopTargetButton = findViewById(R.id.button_stop_target);
        stopNumbersButton = findViewById(R.id.button_stop_numbers);
        confirmButton = findViewById(R.id.button_confirm_expression);
    }

    private void configureSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeView.setText(accelerometer == null
                ? R.string.shake_unavailable : R.string.shake_ready);
    }

    private void configureButtons() {
        stopTargetButton.setOnClickListener(v -> stopTarget());
        stopNumbersButton.setOnClickListener(v -> stopNumbers());
        findViewById(R.id.button_clear_expression).setOnClickListener(v -> clearExpression());
        confirmButton.setOnClickListener(v -> submitExpression());

        int[] numberIds = {
                R.id.button_num_1, R.id.button_num_2, R.id.button_num_3,
                R.id.button_num_4, R.id.button_num_5, R.id.button_num_6
        };
        numberButtons = new Button[numberIds.length];
        for (int i = 0; i < numberIds.length; i++) {
            final int index = i;
            numberButtons[i] = findViewById(numberIds[i]);
            numberButtons[i].setOnClickListener(v -> appendNumber(index));
        }

        int[] operatorIds = {
                R.id.button_op_open, R.id.button_op_close, R.id.button_op_plus,
                R.id.button_op_minus, R.id.button_op_multiply, R.id.button_op_divide
        };
        String[] operators = {"(", ")", "+", "-", "*", "/"};
        for (int i = 0; i < operatorIds.length; i++) {
            String operator = operators[i];
            Button button = findViewById(operatorIds[i]);
            button.setText(operator);
            button.setOnClickListener(v -> appendToken(operator));
        }
    }

    private void startNewRound() {
        cancelTimers();
        gameRound = generator.generate();
        currentPlayer = 1;
        playerOneResult = null;
        playerTwoResult = null;
        targetView.setText(R.string.target_placeholder);
        numbersView.setText(R.string.numbers_placeholder);
        stopTargetButton.setEnabled(true);
        stopNumbersButton.setEnabled(false);
        setPlayControlsEnabled(false);
        clearExpression();
        renderHeader();
        renderScore();
        startTargetRolling();
    }

    private void startTargetRolling() {
        stage = Stage.TARGET_ROLLING;
        rollingHandler.post(targetRoller);
        startAutoStop(this::stopTarget);
    }

    private final Runnable targetRoller = new Runnable() {
        @Override
        public void run() {
            if (stage != Stage.TARGET_ROLLING) {
                return;
            }
            targetView.setText(String.valueOf(100 + displayRandom.nextInt(900)));
            rollingHandler.postDelayed(this, 80);
        }
    };

    private final Runnable numbersRoller = new Runnable() {
        @Override
        public void run() {
            if (stage != Stage.NUMBERS_ROLLING) {
                return;
            }
            numbersView.setText(String.format(Locale.getDefault(), "%d  %d  %d  %d  %d  %d",
                    1 + displayRandom.nextInt(9), 1 + displayRandom.nextInt(9),
                    1 + displayRandom.nextInt(9), 1 + displayRandom.nextInt(9),
                    new int[]{10, 15, 20}[displayRandom.nextInt(3)],
                    new int[]{25, 50, 75, 100}[displayRandom.nextInt(4)]));
            rollingHandler.postDelayed(this, 80);
        }
    };

    private void startAutoStop(Runnable action) {
        if (stageTimer != null) {
            stageTimer.cancel();
        }
        stageTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerView.setText(getString(R.string.auto_stop_value,
                        (int) Math.ceil(millisUntilFinished / 1000.0)));
            }

            @Override
            public void onFinish() {
                action.run();
            }
        }.start();
    }

    private void stopTarget() {
        if (stage != Stage.TARGET_ROLLING) {
            return;
        }
        cancelStageTimer();
        rollingHandler.removeCallbacks(targetRoller);
        targetView.setText(String.valueOf(gameRound.getTarget()));
        stopTargetButton.setEnabled(false);
        stopNumbersButton.setEnabled(true);
        stage = Stage.NUMBERS_ROLLING;
        rollingHandler.post(numbersRoller);
        startAutoStop(this::stopNumbers);
    }

    private void stopNumbers() {
        if (stage != Stage.NUMBERS_ROLLING) {
            return;
        }
        cancelStageTimer();
        rollingHandler.removeCallbacks(numbersRoller);
        int[] numbers = gameRound.getNumbers();
        numbersView.setText(formatNumbers(numbers));
        stopNumbersButton.setEnabled(false);
        for (int i = 0; i < numberButtons.length; i++) {
            numberButtons[i].setText(String.valueOf(numbers[i]));
        }
        startPlayerTurn(1);
    }

    private void startPlayerTurn(int player) {
        currentPlayer = player;
        stage = Stage.PLAYING;
        clearExpression();
        setPlayControlsEnabled(true);
        remainingMillis = 60_000;
        renderHeader();
        startPlayTimer();
    }

    private void startPlayTimer() {
        if (playTimer != null) {
            playTimer.cancel();
        }
        playTimer = new CountDownTimer(remainingMillis, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                long seconds = (long) Math.ceil(millisUntilFinished / 1000.0);
                timerView.setText(String.format(Locale.getDefault(), "%02d:%02d",
                        seconds / 60, seconds % 60));
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                savePlayerResult(null);
            }
        }.start();
    }

    private void appendNumber(int index) {
        if (stage != Stage.PLAYING || !numberButtons[index].isEnabled()) {
            return;
        }
        appendToken(numberButtons[index].getText().toString());
        numberButtons[index].setEnabled(false);
    }

    private void appendToken(String token) {
        if (stage != Stage.PLAYING) {
            return;
        }
        expressionTokens.add(token);
        renderExpression();
    }

    private void clearExpression() {
        expressionTokens.clear();
        if (numberButtons != null) {
            for (Button numberButton : numberButtons) {
                numberButton.setEnabled(stage == Stage.PLAYING);
            }
        }
        renderExpression();
    }

    private void renderExpression() {
        if (expressionView == null) {
            return;
        }
        expressionView.setText(expressionTokens.isEmpty()
                ? getString(R.string.empty_expression) : String.join(" ", expressionTokens));
    }

    private void submitExpression() {
        String expression = String.join(" ", expressionTokens);
        ExpressionEvaluator.Result result = ExpressionEvaluator.evaluate(expression, gameRound.getNumbers());
        if (!result.valid) {
            showInfoDialog(getString(R.string.invalid_expression_title), result.error);
            return;
        }
        savePlayerResult(result.value);
    }

    private void savePlayerResult(Integer value) {
        if (stage != Stage.PLAYING) {
            return;
        }
        if (playTimer != null) {
            playTimer.cancel();
        }
        if (currentPlayer == 1) {
            playerOneResult = value;
            showInfoDialog(getString(R.string.player_result_title, 1), formatResult(value));
            startPlayerTurn(2);
        } else {
            playerTwoResult = value;
            showInfoDialog(getString(R.string.player_result_title, 2), formatResult(value));
            scoreRound();
        }
    }

    private void scoreRound() {
        stage = Stage.FINISHED;
        setPlayControlsEnabled(false);
        int target = gameRound.getTarget();
        boolean p1Exact = playerOneResult != null && playerOneResult == target;
        boolean p2Exact = playerTwoResult != null && playerTwoResult == target;
        if (p1Exact) {
            playerOneScore += 10;
        }
        if (p2Exact) {
            playerTwoScore += 10;
        }
        if (!p1Exact && !p2Exact) {
            int p1Distance = playerOneResult == null ? Integer.MAX_VALUE
                    : Math.abs(target - playerOneResult);
            int p2Distance = playerTwoResult == null ? Integer.MAX_VALUE
                    : Math.abs(target - playerTwoResult);
            if (p1Distance < p2Distance) {
                playerOneScore += 5;
            } else if (p2Distance < p1Distance) {
                playerTwoScore += 5;
            } else if (p1Distance != Integer.MAX_VALUE) {
                if (roundIndex == 0) {
                    playerOneScore += 5;
                } else {
                    playerTwoScore += 5;
                }
            }
        }
        renderScore();
        String summary = getString(R.string.my_number_round_summary,
                formatResult(playerOneResult), formatResult(playerTwoResult),
                playerOneScore, playerTwoScore);
        if (roundIndex == 0) {
            showInfoDialog(getString(R.string.round_result), summary);
            roundIndex = 1;
            targetView.postDelayed(this::startNewRound, 1400);
        } else {
            showFinishDialog(getString(R.string.game_result), summary + "\n" + winnerText());
        }
    }

    private String winnerText() {
        if (playerOneScore == playerTwoScore) {
            return getString(R.string.draw_result);
        }
        return getString(R.string.player_wins, playerOneScore > playerTwoScore ? 1 : 2);
    }

    private String formatResult(Integer value) {
        return value == null ? getString(R.string.no_answer) : String.valueOf(value);
    }

    private String formatNumbers(int[] values) {
        return String.format(Locale.getDefault(), "%d  %d  %d  %d  %d  %d",
                values[0], values[1], values[2], values[3], values[4], values[5]);
    }

    private void renderHeader() {
        headerView.setText(getString(R.string.my_number_header_value,
                roundIndex + 1, currentPlayer, roundIndex + 1));
    }

    private void renderScore() {
        pointsView.setText(getString(R.string.two_player_score, playerOneScore, playerTwoScore));
    }

    private void setPlayControlsEnabled(boolean enabled) {
        confirmButton.setEnabled(enabled);
        findViewById(R.id.button_clear_expression).setEnabled(enabled);
        int[] operatorIds = {
                R.id.button_op_open, R.id.button_op_close, R.id.button_op_plus,
                R.id.button_op_minus, R.id.button_op_multiply, R.id.button_op_divide
        };
        for (int id : operatorIds) {
            findViewById(id).setEnabled(enabled);
        }
        if (numberButtons != null) {
            for (Button button : numberButtons) {
                button.setEnabled(enabled);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        long now = System.currentTimeMillis();
        if (acceleration > 2.4f && now - lastShakeTime > 900) {
            lastShakeTime = now;
            if (stage == Stage.TARGET_ROLLING) {
                stopTarget();
                showToast(R.string.shake_detected);
            } else if (stage == Stage.NUMBERS_ROLLING) {
                stopNumbers();
                showToast(R.string.shake_detected);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    private void cancelStageTimer() {
        if (stageTimer != null) {
            stageTimer.cancel();
            stageTimer = null;
        }
    }

    private void cancelTimers() {
        cancelStageTimer();
        if (playTimer != null) {
            playTimer.cancel();
            playTimer = null;
        }
        rollingHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        cancelTimers();
        super.onDestroy();
    }
}
