package com.project.vortex;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class DeviceItemSettings extends AppCompatActivity {
    private String TAG = "DeviceItemSettings";
    private String deviceName;
    private String deviceAddress;
    private LinearLayout renameDeviceButton, disconnectButton, forgetButton;
    private TextView connectDisconnectText, deviceNameText;
    private ImageView deviceIcon;
    private ImageButton backButton;
    private FrameLayout backButtonFrame;
    private boolean accurateDeviceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_item_settings);

        renameDeviceButton = findViewById(R.id.rename_device_button);
        disconnectButton = findViewById(R.id.connect_disconnect_button);
        forgetButton = findViewById(R.id.forget_button);
        connectDisconnectText = findViewById(R.id.connect_disconnect_text);
        deviceIcon = findViewById(R.id.device_icon);
        backButton = findViewById(R.id.back_button);
        backButtonFrame = findViewById(R.id.back_button_frame);
        RelativeLayout root = findViewById(R.id.device_settings);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    i.left,
                    i.top,
                    i.right,
                    i.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent != null) {
            deviceName = intent.getStringExtra("deviceName");
            deviceAddress = intent.getStringExtra("deviceAddress");
        }
        if(PreferencesManager.getInstance(this).getDeviceCharFlag(deviceAddress)) {
            deviceIcon.setImageResource(R.drawable.ic_led_strip);
        } else {
            deviceIcon.setImageResource(R.drawable.ic_device_default);
        }
        deviceNameText = findViewById(R.id.device_name_text);
        deviceNameText.setText(deviceName);

        View.OnClickListener sharedBackListener = v -> {
            Log.d(TAG, "Back button clicked");
            int centerX = backButtonFrame.getWidth() / 2;
            int centerY = backButtonFrame.getHeight() / 2;

            backButtonFrame.setPressed(true);
            Drawable foreground = backButtonFrame.getForeground();
            if(foreground instanceof RippleDrawable){
                (foreground).setHotspot(centerX, centerY);
            }

            backButtonFrame.postDelayed(() -> {
                backButtonFrame.setPressed(false);
                setResult(RESULT_OK);
                finish();
            }, 200);
        };
        backButton.setOnClickListener(sharedBackListener);
        backButtonFrame.setOnClickListener(sharedBackListener);

        //Update UI
        updateUI();
    }
    private void updateUI(){
        //Handle Rename device
        renameDeviceButton.setOnClickListener(v -> showRenameDialog(deviceAddress));

        //Handle Disconnect device
        disconnectButton.setOnClickListener(v -> {
            if(ConnectedDeviceManager.getInstance().getDeviceStatus(deviceAddress)) {
                Log.d(TAG, "Disconnecting device: " + deviceName);
                Intent disconnectIntent = new Intent(this, BLEService.class);
                disconnectIntent.putExtra("management_BLEService", "DISCONNECT");
                disconnectIntent.putExtra("device_address", deviceAddress);
                connectDisconnectText.setText("Connect");
                startService(disconnectIntent);
                setResult(RESULT_OK);
                Toast.makeText(this, "Device disconnected!", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Connecting device: " + deviceName);
                Intent connectIntent = new Intent(this, BLEService.class);
                connectIntent.putExtra("management_BLEService", "CONNECT");
                connectIntent.putExtra("device_address", deviceAddress);
                connectDisconnectText.setText("Disconnect");
                startService(connectIntent);
                Toast.makeText(this, "Device connecting!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
            }
            Log.d(TAG, "Device status updated: " + deviceName + " " + ConnectedDeviceManager.getInstance().getDeviceStatus(deviceAddress) + " ConnectDisconnectText: " + connectDisconnectText.getText().toString());
        });

        //Handle Forget device
        forgetButton.setOnClickListener(v -> {
            setResult(RESULT_OK);
            if(PreferencesManager.getInstance(this).getDeviceStatus(deviceAddress).equals("Connected")){
                Intent disconnectIntent = new Intent(this, BLEService.class);
                disconnectIntent.putExtra("management_BLEService", "FORGET");
                disconnectIntent.putExtra("device_address", deviceAddress);
                startService(disconnectIntent);
            }
            Log.d(TAG, "Forgetting device: " + deviceName + " (" + deviceAddress + ")");
            PreferencesManager.getInstance(this).removeDevice(deviceAddress);
            Toast.makeText(this, "Device removed!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    private void showRenameDialog(String deviceAddress) {
        String currentName = PreferencesManager.getInstance(this).getDeviceName(deviceAddress);

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_rename);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText input = dialog.findViewById(R.id.dialog_edit_text);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);
        Button renameButton = dialog.findViewById(R.id.rename_button);

        input.setText(currentName);
        input.setSelection(currentName.length());

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                renameButton.performClick();
                return true;
            }
            return false;
        });

        renameButton.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                Log.d(TAG, "Renaming device: " + currentName + " -> " + newName);
                PreferencesManager.getInstance(this).saveDevice(
                        deviceAddress,
                        newName,
                        PreferencesManager.getInstance(this).getDeviceCharFlag(deviceAddress),
                        PreferencesManager.getInstance(this).getDeviceStatus(deviceAddress)
                );
                deviceName = newName;
                deviceNameText.setText(newName);
                Toast.makeText(this, "Device renamed!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                v.postDelayed(dialog::dismiss,200);
            } else {
                Toast.makeText(this, "Enter a valid name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        LocalBroadcastManager.getInstance(this).registerReceiver(isDeviceConnectedReceiver,
                new IntentFilter("isDeviceConnected"));
        accurateDeviceStatus = ConnectedDeviceManager.getInstance().getDeviceStatus(deviceAddress);
        if(accurateDeviceStatus) {
            connectDisconnectText.setText("Disconnect");
        } else {
            connectDisconnectText.setText("Connect");
        }
        Log.d(TAG, "Status updated for deice: " + deviceAddress + " " + accurateDeviceStatus + " ConnectDisconnectText: " + connectDisconnectText.getText().toString());
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(isDeviceConnectedReceiver);
    }
    private final BroadcastReceiver isDeviceConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = intent.getBooleanExtra("isConnected", false);
            Log.d(TAG, "isDeviceConnectedReceiver called with isConnected: " + isConnected);
            String deviceAddress = ConnectedDeviceManager.getInstance().getDeviceAddress();
            accurateDeviceStatus = ConnectedDeviceManager.getInstance().getDeviceStatus(deviceAddress);
            Log.d(TAG, "isConnected: " + isConnected);;
            Log.d(TAG, "Status updated for deice: " + deviceAddress + " " + accurateDeviceStatus);
            if(accurateDeviceStatus) {
                connectDisconnectText.setText("Disconnect");
            } else {
                connectDisconnectText.setText("Connect");
            }
        }
    };
}
