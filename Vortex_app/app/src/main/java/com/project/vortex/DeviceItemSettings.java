package com.project.vortex;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DeviceItemSettings extends AppCompatActivity {
    private String TAG = "DeviceItemSettings";
    private String deviceName;
    private String deviceAddress;

    private LinearLayout renameDeviceButton, disconnectButton, forgetButton;
    private TextView connectDisconnectText, deviceNameText;
    private ImageView deviceIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_item_settings);

        renameDeviceButton = findViewById(R.id.rename_device_button);
        disconnectButton = findViewById(R.id.connect_disconnect_button);
        forgetButton = findViewById(R.id.forget_button);
        connectDisconnectText = findViewById(R.id.connect_disconnect_text);
        deviceIcon = findViewById(R.id.device_icon);

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

        //Handle Rename device
        renameDeviceButton.setOnClickListener(v -> showRenameDialog(deviceAddress));

        //Handle Disconnect device
        disconnectButton.setOnClickListener(v -> {
            if(PreferencesManager.getInstance(this).getDeviceStatus(deviceAddress).equals("Connected")) {
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
        });

        //Handle Forget device
        forgetButton.setOnClickListener(v -> {
            Log.d(TAG, "Forgetting device: " + deviceName + " (" + deviceAddress + ")");
            PreferencesManager.getInstance(this).removeDevice(deviceAddress);
            setResult(RESULT_OK);
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

        renameButton.setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                Log.d(TAG, "Renaming device: " + currentName + " -> " + newName);
                PreferencesManager.getInstance(this).saveDevice(
                        deviceAddress,
                        newName,
                        ConnectedDeviceManager.getInstance().getDeviceCharFlag(deviceAddress),
                        PreferencesManager.getInstance(this).getDeviceStatus(deviceAddress)
                );
                deviceName = newName;
                deviceNameText.setText(newName);
                Toast.makeText(this, "Device renamed!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Enter a valid name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
