package com.example.mobilnekt1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import com.example.mobilnekt1.games.mynumber.ExpressionEvaluator;
import com.example.mobilnekt1.games.mynumber.multiplayer.MyNumberGameListener;
import com.example.mobilnekt1.games.mynumber.multiplayer.MyNumberGameRepository;
import com.example.mobilnekt1.games.mynumber.multiplayer.MyNumberGameState;
import com.example.mobilnekt1.games.shared.GameActionCallback;
import com.example.mobilnekt1.profile.data.StatsRepository;
import com.example.mobilnekt1.match.data.MatchScoreRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class MultiplayerMyNumberActivity extends BaseKt1Activity
        implements SensorEventListener {
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final List<String> expressionTokens = new ArrayList<>();
    private MyNumberGameRepository repository;
    private StatsRepository statsRepository;
    private MatchScoreRepository matchScoreRepository;
    private MyNumberGameState state;
    private String matchId;
    private TextView headerView;
    private TextView timerView;
    private TextView targetView;
    private TextView numbersView;
    private TextView expressionView;
    private TextView scoreView;
    private TextView shakeView;
    private Button stopTargetButton;
    private Button stopNumbersButton;
    private Button confirmButton;
    private Button clearButton;
    private Button[] numberButtons;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShake;
    private boolean advancing;
    private boolean resultShown;
    private boolean statsCommitted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_number);
        matchId = getIntent().getStringExtra(MatchLobbyActivity.EXTRA_MATCH_ID);
        if (matchId == null) {
            finish();
            return;
        }
        bindViews();
        configureButtons();
        configureSensor();
        repository = new MyNumberGameRepository(this);
        statsRepository = new StatsRepository(this);
        matchScoreRepository = new MatchScoreRepository(this);
        repository.listen(matchId, new MyNumberGameListener() {
            @Override
            public void onChanged(MyNumberGameState newState) {
                boolean changedRound = state != null && state.round != newState.round;
                state = newState;
                advancing = false;
                if (changedRound) clearExpression();
                render();
                if (newState.isFinished() && !statsCommitted) {
                    statsCommitted = true;
                    statsRepository.commitMyNumber(matchId, silentCallback());
                    matchScoreRepository.commitGameResult(
                            matchId, "myNumber", "phase", silentCallback());
                }
            }

            @Override
            public void onError(String message) {
                showInfoDialog(getString(R.string.match_error_title), message);
            }
        });
        repository.initialize(matchId, silentCallback());
        ticker.post(tick);
    }

    private void bindViews() {
        headerView = findViewById(R.id.text_my_number_header);
        timerView = findViewById(R.id.text_my_number_timer);
        targetView = findViewById(R.id.text_target_number);
        numbersView = findViewById(R.id.text_offered_numbers);
        expressionView = findViewById(R.id.text_expression);
        scoreView = findViewById(R.id.text_my_number_points);
        shakeView = findViewById(R.id.text_shake_status);
        stopTargetButton = findViewById(R.id.button_stop_target);
        stopNumbersButton = findViewById(R.id.button_stop_numbers);
        confirmButton = findViewById(R.id.button_confirm_expression);
        clearButton = findViewById(R.id.button_clear_expression);
    }

    private void configureButtons() {
        stopTargetButton.setOnClickListener(v -> repository.stopTarget(matchId, silentCallback()));
        stopNumbersButton.setOnClickListener(v -> repository.stopNumbers(matchId, silentCallback()));
        clearButton.setOnClickListener(v -> clearExpression());
        confirmButton.setOnClickListener(v -> submit());
        int[] ids = {R.id.button_num_1, R.id.button_num_2, R.id.button_num_3,
                R.id.button_num_4, R.id.button_num_5, R.id.button_num_6};
        numberButtons = new Button[ids.length];
        for (int i = 0; i < ids.length; i++) {
            int index = i;
            numberButtons[i] = findViewById(ids[i]);
            numberButtons[i].setOnClickListener(v -> appendNumber(index));
        }
        int[] operatorIds = {R.id.button_op_open, R.id.button_op_close, R.id.button_op_plus,
                R.id.button_op_minus, R.id.button_op_multiply, R.id.button_op_divide};
        String[] operators = {"(", ")", "+", "-", "*", "/"};
        for (int i = 0; i < operatorIds.length; i++) {
            Button button = findViewById(operatorIds[i]);
            String operator = operators[i];
            button.setText(operator);
            button.setOnClickListener(v -> appendToken(operator));
        }
    }

    private void configureSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void appendNumber(int index) {
        if (!canPlay() || !numberButtons[index].isEnabled()) return;
        appendToken(numberButtons[index].getText().toString());
        numberButtons[index].setEnabled(false);
    }

    private void appendToken(String token) {
        if (!canPlay()) return;
        expressionTokens.add(token);
        renderExpression();
    }

    private void clearExpression() {
        expressionTokens.clear();
        for (Button button : numberButtons) button.setEnabled(canPlay());
        renderExpression();
    }

    private void renderExpression() {
        expressionView.setText(expressionTokens.isEmpty()
                ? getString(R.string.empty_expression) : String.join(" ", expressionTokens));
    }

    private void submit() {
        ExpressionEvaluator.Result result = ExpressionEvaluator.evaluate(
                String.join(" ", expressionTokens), state.numberArray());
        if (!result.valid) {
            showInfoDialog(getString(R.string.invalid_expression_title), result.error);
            return;
        }
        setPlayControls(false);
        repository.submitResult(matchId, result.value, new GameActionCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                render();
                showInfoDialog(getString(R.string.match_error_title), message);
            }
        });
    }

    private void render() {
        if (state == null) return;
        boolean starter = repository.currentUserId() != null
                && repository.currentUserId().equals(state.startingPlayerId);
        headerView.setText(getString(R.string.multiplayer_my_number_header,
                state.round + 1, starter ? getString(R.string.you) : getString(R.string.opponent)));
        scoreView.setText(getString(R.string.two_player_score,
                (int) state.player1Score, (int) state.player2Score));
        boolean targetVisible = !"targetRolling".equals(state.phase);
        boolean numbersVisible = "playing".equals(state.phase) || state.isFinished();
        targetView.setText(targetVisible ? String.valueOf(state.target) : "---");
        numbersView.setText(numbersVisible ? formatNumbers(state.numberArray()) : "-- -- -- -- -- --");
        if (numbersVisible) {
            int[] numbers = state.numberArray();
            for (int i = 0; i < Math.min(numbers.length, numberButtons.length); i++) {
                numberButtons[i].setText(String.valueOf(numbers[i]));
            }
        }
        stopTargetButton.setEnabled(starter && "targetRolling".equals(state.phase));
        stopNumbersButton.setEnabled(starter && "numbersRolling".equals(state.phase));
        shakeView.setText(starter && ("targetRolling".equals(state.phase)
                || "numbersRolling".equals(state.phase))
                ? R.string.shake_ready : R.string.waiting_for_round_starter);
        setPlayControls(canPlay());
        if (state.isFinished() && !resultShown) {
            resultShown = true;
            showFinishDialog(getString(R.string.game_result), getString(R.string.two_player_score,
                    (int) state.player1Score, (int) state.player2Score));
        }
    }

    private boolean canPlay() {
        return state != null && "playing".equals(state.phase)
                && !state.hasSubmitted(repository.currentUserId());
    }

    private void setPlayControls(boolean enabled) {
        confirmButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        int[] operatorIds = {R.id.button_op_open, R.id.button_op_close, R.id.button_op_plus,
                R.id.button_op_minus, R.id.button_op_multiply, R.id.button_op_divide};
        for (int id : operatorIds) findViewById(id).setEnabled(enabled);
        for (Button button : numberButtons) button.setEnabled(enabled);
    }

    private String formatNumbers(int[] values) {
        if (values.length < 6) return "-- -- -- -- -- --";
        return String.format(Locale.getDefault(), "%d  %d  %d  %d  %d  %d",
                values[0], values[1], values[2], values[3], values[4], values[5]);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (state != null && !state.isFinished()) {
                long remaining = Math.max(0, state.deadlineMillis - System.currentTimeMillis());
                long seconds = (long) Math.ceil(remaining / 1000.0);
                timerView.setText(String.format(Locale.getDefault(), "%02d:%02d",
                        seconds / 60, seconds % 60));
                if ("targetRolling".equals(state.phase)) {
                    targetView.setText(String.valueOf(100 + random.nextInt(900)));
                } else if ("numbersRolling".equals(state.phase)) {
                    numbersView.setText(getString(R.string.rolling_numbers));
                }
                if (remaining == 0 && !advancing) {
                    advancing = true;
                    if ("targetRolling".equals(state.phase)) repository.stopTarget(matchId, silentCallback());
                    else if ("numbersRolling".equals(state.phase)) repository.stopNumbers(matchId, silentCallback());
                    else if ("playing".equals(state.phase)) repository.finishExpired(matchId, silentCallback());
                }
            }
            ticker.postDelayed(this, 250);
        }
    };

    private GameActionCallback silentCallback() {
        return new GameActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) {
                advancing = false;
                showInfoDialog(getString(R.string.match_error_title), message);
            }
        };
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (state == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER
                || !repository.currentUserId().equals(state.startingPlayerId)) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        float force = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        long now = System.currentTimeMillis();
        if (force > 2.4f && now - lastShake > 900) {
            lastShake = now;
            if ("targetRolling".equals(state.phase)) repository.stopTarget(matchId, silentCallback());
            else if ("numbersRolling".equals(state.phase)) repository.stopNumbers(matchId, silentCallback());
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ticker.removeCallbacksAndMessages(null);
        repository.stopListening();
        super.onDestroy();
    }
}
