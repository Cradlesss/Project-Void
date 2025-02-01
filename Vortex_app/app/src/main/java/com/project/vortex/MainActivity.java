package com.project.vortex;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //TAG
    private static final String TAG = "MainActivity";
    // BLE
    private BluetoothAdapter bluetoothAdapter;
    // UI
    private Button selectDeviceButton, openAnimationsButton, disconnectButton, staticColorButton;
    private TextView statusText;
    private SeekBar brightnessSeekBar;
    private BottomNavigationView bottomNavigationView;
    private String status;
    private String deviceName;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private boolean isConnected = false;
    //seek bar stuff
    private int lastSentCommand = -1;
    private static final int SKIP_THRESHOLD = 5;
    private int minSeekBarValue = 48;
    private int maxSeekBarValue = 60;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing BroadcastReceivers
        LocalBroadcastManager.getInstance(this).registerReceiver(bleStatusReceiver,
                new IntentFilter("BLEServiceStatusUpdate"));

        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "ConnectionStateManager isConnected: " + isConnected);
        deviceName = ConnectedDeviceManager.getInstance().getDeviceName();
        Log.d(TAG, "Connected device name: " + deviceName);

        // Initialize UI
        registerUI();
        openAnimationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Animations.class);
            startActivity(intent);
        });
        staticColorButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StaticColorActivity.class);
            startActivity(intent);
        });
        //Navigation bar
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                return true;
            } else if (itemId == R.id.animations) {
                Intent intent = new Intent(this, Animations.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.settings) {
                Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
                return false;
            } else if(itemId == R.id.static_color){
                Intent intent = new Intent(this, StaticColorActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        //disconnect button
        disconnectButton.setOnClickListener(v -> disconnectDevice());
        //brightnessSeekBar
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) { //Only User commands
                    if(isConnected) {
                        Log.d(TAG, "Progress changed:");
                        //skip some to avoid overloading the device
                        if (Math.abs(progress - lastSentCommand) >= SKIP_THRESHOLD) {
                            Log.d("onProgressChanged", "Command sent");

                            //Delayed by 100ms command to send brightness value
                            sendBrightness(progress);
                            lastSentCommand = progress;
                        }
                    } else {
                        seekBar.setProgress(lastSentCommand);
                  }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(!isConnected){
                    lastSentCommand = seekBar.getProgress();
                    Toast.makeText(MainActivity.this, "Connect to a device first", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(!isConnected){
                    seekBar.setProgress(lastSentCommand);
                } else {
                    Log.d(TAG, "Progress stopped");
                    new Handler().postDelayed(() -> sendBrightness(lastSentCommand), 200);
                    new Handler().postDelayed(() -> sendBrightness(14), 400);
                }

            }
        });

        // Request permissions
        if (!checkPermissions()) {
            requestPermissions();
        }

        // Initialize Bluetooth
        bluetoothAdapter = ((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        // Check Bluetooth availability
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Button listeners
        selectDeviceButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectDeviceActivity.class);
            startActivity(intent);
        });
    }
    private void registerUI(){
        selectDeviceButton = findViewById(R.id.selectDeviceBtn);
        openAnimationsButton = findViewById(R.id.goToAnimations);
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        disconnectButton = findViewById(R.id.disconnectBtn);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.home);
        staticColorButton = findViewById(R.id.staticColorBtn);
        brightnessSeekBar.setMax(maxSeekBarValue - minSeekBarValue);
        setUIEnabled(false);
        statusText = findViewById(R.id.statusText);
        statusText.setText(R.string.default_statusText);
    }

    private boolean checkPermissions() {
        boolean hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Log.d("Permissions", "Fine Location: " + hasFineLocation + ", Coarse Location: " + hasCoarseLocation);

        return hasFineLocation || hasCoarseLocation;
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.FOREGROUND_SERVICE);

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean permissionsGranted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = true;
                    break;
                }
            }

            if (permissionsGranted) {
                Toast.makeText(this, R.string.yes_permissions, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.no_permissions, Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void sendBrightness(int brightnessValue) {
        Intent sendInitiatorValue = new Intent(this, BLEService.class);
        sendInitiatorValue.putExtra("management_BLEService", "DRIVER_COMMAND");
        sendInitiatorValue.putExtra("command", 12);
        startService(sendInitiatorValue); // Send brightness via BLE service

        //Delayed by 100ms command to send brightness value
        new Handler().postDelayed(() -> {
            int finalProgress = brightnessValue + minSeekBarValue;
            Intent intent = new Intent(this, BLEService.class);
            intent.putExtra("management_BLEService", "DRIVER_COMMAND");
            intent.putExtra("command", finalProgress);
            startService(intent); // Send command via BLE service
            Log.d(TAG, "Brightness sent: " + finalProgress);
        }, 100);
    }
    public void setUIEnabled(boolean enabled) {
        disconnectButton.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!isFinishing()){
            return;
        }
        Log.d(TAG, "MainActivity destroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.home);
        LocalBroadcastManager.getInstance(this).registerReceiver(bleStatusReceiver,
                new IntentFilter("BLEServiceStatusUpdate"));
        deviceName = ConnectedDeviceManager.getInstance().getDeviceName();
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "ConnectionStateManager isConnected: " + isConnected);
        setUIEnabled(isConnected);
        if(isConnected){
            statusText.setText("Connected to: " + deviceName);
        }
        else {
            statusText.setText(R.string.default_statusText);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bleStatusReceiver);
    }
    private BroadcastReceiver bleStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("status")){
                status = intent.getStringExtra("status");
                statusText.setText(status);
            }
        }
    };
    private void disconnectDevice() {
        if(isConnected) {
            //stop service
            stopService(new Intent(this, BLEService.class));
            //disable UI
            setUIEnabled(false);
            Toast.makeText(this, R.string.disconnected, Toast.LENGTH_SHORT).show();
            statusText.setText(R.string.default_statusText);
        } else {
            Toast.makeText(this, R.string.handleCommand, Toast.LENGTH_SHORT).show();
        }
    }
}
