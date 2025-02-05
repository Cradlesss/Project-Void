package com.project.vortex;

import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Animations extends AppCompatActivity {
    private static final String TAG = "Animations";
    private boolean isConnected = false; // Tracks connection state
    BottomNavigationView bottomNavigationView;

    Button offButton, twinkleButton, jinxButton, transitionButton, bluebButton, rainbowButton, beatrButton, runredButton, rawNoiseButton, movingRainbowButton, waveButton, blightbButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.animations);

        // Initialize connection state from ConnectionStateManager
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Initial connection status: " + isConnected);

        // Initialize UI
        InitializeUI();

        // Navigation bar setup
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.animations) {
                return true;
            } else if (itemId == R.id.settings) {
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
                return false;
            } else if (itemId == R.id.static_color) {
                Intent intent = new Intent(this, StaticColorActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        AnimatorSet animatorSet = new AnimatorSet();
        if(animatorSet.isRunning()){
            Log.d(TAG, "AnimatorSet is running");
            animatorSet.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the connection state whenever the activity is resumed
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Connection state updated in onResume: " + isConnected);

        // Update UI based on the connection state (optional)
        updateUIBasedOnConnectionState(isConnected);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.animations);
    }

    private void updateUIBasedOnConnectionState(boolean isConnected) {
        if (!isConnected) {
            Toast.makeText(this, R.string.updateUIBasedOnConnectionState, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCommand(int command) {
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("management_BLEService", "DRIVER_COMMAND");
        intent.putExtra("command", command);
        startService(intent); // Send command via BLE service
    }

    private void InitializeUI() {
        offButton = findViewById(R.id.offBtn);
        twinkleButton = findViewById(R.id.twinkleBtn);
        jinxButton = findViewById(R.id.jinxBtn);
        transitionButton = findViewById(R.id.transitionBreakBtn);
        bluebButton = findViewById(R.id.bluebBtn);
        rainbowButton = findViewById(R.id.rainbowBtn);
        beatrButton = findViewById(R.id.beatrBtn);
        runredButton = findViewById(R.id.runredBtn);
        rawNoiseButton = findViewById(R.id.rawNoiseBtn);
        movingRainbowButton = findViewById(R.id.movingRainbowBtn);
        waveButton = findViewById(R.id.waveBtn);
        blightbButton = findViewById(R.id.blightbBtn);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.animations);

        setButtonListeners();
    }

    private void setButtonListeners() {
        offButton.setOnClickListener(v -> handleCommand(0));
        twinkleButton.setOnClickListener(v -> handleCommand(1));
        jinxButton.setOnClickListener(v -> handleCommand(2));
        transitionButton.setOnClickListener(v -> handleCommand(3));
        bluebButton.setOnClickListener(v -> handleCommand(4));
        rainbowButton.setOnClickListener(v -> handleCommand(5));
        beatrButton.setOnClickListener(v -> handleCommand(6));
        runredButton.setOnClickListener(v -> handleCommand(7));
        rawNoiseButton.setOnClickListener(v -> handleCommand(8));
        movingRainbowButton.setOnClickListener(v -> handleCommand(9));
        waveButton.setOnClickListener(v -> handleCommand(10));
        blightbButton.setOnClickListener(v -> handleCommand(11));
    }
    private void enableButtons(boolean isEnabled) {
        offButton.setEnabled(isEnabled);
        twinkleButton.setEnabled(isEnabled);
        jinxButton.setEnabled(isEnabled);
        transitionButton.setEnabled(isEnabled);
        bluebButton.setEnabled(isEnabled);
        rainbowButton.setEnabled(isEnabled);
        beatrButton.setEnabled(isEnabled);
        runredButton.setEnabled(isEnabled);
        rawNoiseButton.setEnabled(isEnabled);
        movingRainbowButton.setEnabled(isEnabled);
        waveButton.setEnabled(isEnabled);
        blightbButton.setEnabled(isEnabled);

    }

    private void handleCommand(int command) {
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        if (isConnected) {
            enableButtons(false);
            sendCommand(command);
            new Handler().postDelayed(() -> enableButtons(true), 250);
        } else {
            Toast.makeText(this, R.string.handleCommand, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Paused Animations activity");
    }

    private void disabledButtons() {
        Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
    }
}
