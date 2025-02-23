package com.project.vortex;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SelectDeviceActivity extends AppCompatActivity {
    //TAG
    private static final String TAG = "SelectDeviceActivity";
    //BLE
    private static final UUID SERVICE_UUID = UUID.fromString("895dc926-817a-424d-8736-f582d2dbac8e");
    private static final UUID ANIMATION_CHAR_UUID = UUID.fromString("7953deb4-b2e1-4829-a692-8ec173cc71fc");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isConnected = false;
    //UI
    private Map<String, Boolean> deviceCharFlagMap = new HashMap<>();
    private ProgressBar progressBar;
    private LinearLayout deviceContainer;
    private BottomNavigationView bottomNavigationView;
    private Button scanButton;
    private ImageButton burgerMenuButton, backButton;
    private FrameLayout backButtonFrame, burgerMenuButtonFrame;
    private String currentlyConnectedDevice = null;
    private Set<String> deviceSet = new HashSet<>();
    //Device
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private Map<String, String> deviceStatusMap = new HashMap<>(); // To track device statuses

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Initial connection status: " + isConnected);

        progressBar = findViewById(R.id.progressBar);
        deviceContainer = findViewById(R.id.device_container);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        scanButton = findViewById(R.id.scan_button);
        burgerMenuButton = findViewById(R.id.burger_menu_button);
        backButton = findViewById(R.id.back_button);
        backButtonFrame = findViewById(R.id.back_button_frame);
        burgerMenuButtonFrame = findViewById(R.id.burger_menu_button_frame);

        bluetoothAdapter = ((android.bluetooth.BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        loadDiscoveredDevices();

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
                finish();
            }, 200);
        };

        View.OnClickListener sharedBurgerMenuListener = v -> {
            Log.d(TAG, "Burger menu button clicked");
            int centerX = burgerMenuButtonFrame.getWidth() / 2;
            int centerY = burgerMenuButtonFrame.getHeight() / 2;

            burgerMenuButtonFrame.setPressed(true);
            Drawable foreground = burgerMenuButtonFrame.getForeground();
            if(foreground instanceof RippleDrawable){
                (foreground).setHotspot(centerX, centerY);
            }

            burgerMenuButtonFrame.postDelayed(() -> {
                burgerMenuButtonFrame.setPressed(false);
            }, 200);
            popupMenu();
        };

        burgerMenuButton.setOnClickListener(sharedBurgerMenuListener);
        burgerMenuButtonFrame.setOnClickListener(sharedBurgerMenuListener);
        backButton.setOnClickListener(sharedBackListener);
        backButtonFrame.setOnClickListener(sharedBackListener);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        scanButton.setOnClickListener(v -> startDeviceScan());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.animations) {
                Intent intent = new Intent(this, Animations.class);
                startActivity(intent);
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
    }

    private void popupMenu(){
        PopupMenu popupMenu = new PopupMenu(SelectDeviceActivity.this, burgerMenuButton, Gravity.END, 0, R.style.PopupMenuStyle);
        popupMenu.getMenuInflater().inflate(R.menu.burger_menu_popup, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.refresh_device_list) {
                refreshDeviceList();
                return true;
            } else if (itemId == R.id.menu_advanced_settings) {
                Toast.makeText(SelectDeviceActivity.this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
            }
            return false;
        });
        popupMenu.show();
        for(int i = 0; i < popupMenu.getMenu().size(); i++){
            MenuItem menuItem = popupMenu.getMenu().getItem(i);
            SpannableString s = new SpannableString(menuItem.getTitle());
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(SelectDeviceActivity.this, R.color.primaryTextColor)),
                    0,s.length(),0);
            menuItem.setTitle(s);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Connection state updated in onResume: " + isConnected);
        currentlyConnectedDevice = ConnectedDeviceManager.getInstance().getDeviceAddress();
        Log.d(TAG, "currentlyConnectedDevice updated in onResume: " + currentlyConnectedDevice);
        updateUIBasedOnConnectionState(isConnected);
        LocalBroadcastManager.getInstance(this).registerReceiver(isDeviceConnectedReceiver,
                new IntentFilter("isDeviceConnected"));

        loadDiscoveredDevices();
        updateTheUI();
    }
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(isDeviceConnectedReceiver);
    }

    private void updateUIBasedOnConnectionState(boolean isConnected) {
        if (!isConnected) {
            Toast.makeText(this, R.string.updateUIBasedOnConnectionState, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "updateUIBasedOnConnectionState called");
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void startDeviceScan() {
        if (!checkPermissions()) {
            Toast.makeText(this, R.string.bt_permissions_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, BLEService.class);
        stopService(intent);

        progressBar.setVisibility(View.VISIBLE);
        deviceList.clear();
        Log.d("startDeviceScan", "Device list cleared");

        includePairedDevices();

        try {
            List<ScanFilter> filters = new ArrayList<>();

            ScanFilter serviceFilter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                    .build();
            filters.add(serviceFilter);
            Log.d("startDeviceScan", "Number of filters: " + filters.size());

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);

            // Stop scan after 5 seconds
            new Handler().postDelayed(() -> {
                try{
                    bluetoothLeScanner.stopScan(scanCallback);
                } catch (SecurityException e){
                    Log.e(TAG, "Permission denied for stopping scan: " + e.getMessage());
                }
                updateDeviceList();
            }, 5000);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for starting scan: " + e.getMessage());
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();

            if (device != null && !deviceList.contains(device)) {
                    deviceSet.add(device.getAddress());
                    deviceList.add(device);

                    // Temporary connection to discover characteristics
                    device.connectGatt(SelectDeviceActivity.this, false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            if (newState == BluetoothGatt.STATE_CONNECTED) {
                                Log.d(TAG, "Connected to " + gatt.getDevice().getName());
                                gatt.discoverServices();
                            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                                Log.d(TAG, "Disconnected from " + gatt.getDevice().getName());
                                gatt.close();
                            }
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d(TAG, "Services discovered for " + gatt.getDevice().getName());
                                assignIconBasedOnCharacteristic(gatt);
                            } else {
                                Log.e(TAG, "Service discovery failed with status: " + status);
                            }
                            gatt.disconnect(); // Disconnect after discovery
                        }
                    });
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(SelectDeviceActivity.this, R.string.scan_failed, Toast.LENGTH_SHORT).show();
        }
    };

    @SuppressLint("MissingPermission")
    private void assignIconBasedOnCharacteristic(BluetoothGatt gatt) {
        Log.d(TAG, "assignIconBasedOnCharacteristic" + gatt.getDevice().getName() + " " + gatt.getDevice().getAddress());
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        String deviceAddress = gatt.getDevice().getAddress();

        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(ANIMATION_CHAR_UUID);

            if (characteristic != null) {
                Log.d(TAG, "Characteristic found for " + gatt.getDevice().getName());
                // Assign a custom icon for devices with the characteristic
                deviceCharFlagMap.put(deviceAddress, true);
                ConnectedDeviceManager.getInstance().setDeviceCharFlag(deviceAddress, true);
                PreferencesManager.getInstance(this).saveDevice(
                        deviceAddress,
                        gatt.getDevice().getName(),
                        true,
                        "Disconnected"
                );
            } else {
                Log.d(TAG, "Characteristic not found for " + gatt.getDevice().getName());
                // Assign a default icon for devices without the characteristic
                deviceCharFlagMap.put(deviceAddress, false);
                ConnectedDeviceManager.getInstance().setDeviceCharFlag(deviceAddress, false);
                PreferencesManager.getInstance(this).saveDevice(
                        deviceAddress,
                        gatt.getDevice().getName(),
                        false,
                        "Disconnected"
                );
            }
        }
    }
    @SuppressLint("MissingPermission")
    private void includePairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices == null || pairedDevices.isEmpty()) return;

        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();

            Log.d(TAG, "Checking paired device: " + deviceName + " (" + deviceAddress + ")");

            // Connect temporarily to check for characteristic
            BluetoothGatt bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to paired device: " + gatt.getDevice().getName());
                        gatt.discoverServices();
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from paired device: " + gatt.getDevice().getName());
                        gatt.close();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "Services discovered for paired device: " + gatt.getDevice().getName());

                        BluetoothGattService service = gatt.getService(SERVICE_UUID); // Expected UUID for compatibility

                        if (service != null) {
                            Log.d(TAG, "Device " + deviceName + " has required BLE service. Adding to list.");
                            assignIconBasedOnCharacteristic(gatt);
                            deviceSet.add(deviceAddress);
                            deviceList.add(device);
                        } else {
                            Log.d(TAG, "Device " + deviceName + " does not have required BLE service. Skipping.");
                        }
                    } else {
                        Log.e(TAG, "Service discovery failed for paired device: " + gatt.getDevice().getName());
                    }
                    gatt.disconnect(); // Disconnect after checking
                }
            });

            // If the device is incompatible, close the GATT connection immediately
            if (bluetoothGatt == null) {
                Log.d(TAG, "Skipping incompatible device: " + deviceName);
            }
        }
    }



    @SuppressLint("MissingPermission")
    private void updateDeviceList() {
        progressBar.setVisibility(View.GONE);

        if (deviceList.isEmpty()) {
            Toast.makeText(this, R.string.nothing_found, Toast.LENGTH_SHORT).show();
            return;
        }

        saveDiscoveredDevices();

        updateTheUI();
    }

    @SuppressLint("MissingPermission")
    private void updateTheUI() {
        // Clear current UI
        deviceContainer.removeAllViews();
        // LayoutInflater for dynamically adding views
        LayoutInflater inflater = LayoutInflater.from(this);
        int totalDevices = deviceList.size();
        int index = 0;

        // Loop through deviceList to populate UI
        for (BluetoothDevice device : deviceList) {
            View deviceView = inflater.inflate(R.layout.device_list_item, deviceContainer, false);
            TextView deviceName = deviceView.findViewById(R.id.device_name);
            TextView deviceStatus = deviceView.findViewById(R.id.device_status);
            ImageButton settingsButton = deviceView.findViewById(R.id.settings_button);
            ImageView deviceIcon = deviceView.findViewById(R.id.device_icon);
            View divider = deviceView.findViewById(R.id.divider_line);

            String address = device.getAddress();
            String status = PreferencesManager.getInstance(this).getDeviceStatus(address);
            String deviceNameText = PreferencesManager.getInstance(this).getDeviceName(address);
            if(deviceNameText == null) deviceNameText = device.getName();
            if(deviceNameText.equals("Unknown")) deviceNameText = device.getName();
            Log.d("updateTheUI", "Updating UI for device: " + device.getName() + " (" + address + ")" + " currentlyConnectedDevice: " + currentlyConnectedDevice + " isConnected: " + isConnected);
            deviceStatus.setText(status);
            deviceName.setText(deviceNameText);
            Log.d("updateTheUI", "Device updated: " + device.getName() + " (" + address + ")" + " with status: " + status);

            // Assign icons dynamically
            if (deviceCharFlagMap.containsKey(address) && deviceCharFlagMap.get(address)) {
                deviceIcon.setImageResource(R.drawable.ic_led_strip);
            } else {
                deviceIcon.setImageResource(R.drawable.ic_device_default);
            }

            deviceView.setOnClickListener(v -> {
                v.setPressed(true);

                v.postDelayed(() -> {
                    v.setPressed(false);
                    if (!status.equals("Connected")) {
                        connectToDevice(device);
                    } else {
                        disconnectFromDevice(device);
                    }
                },200);
            });

            //Settings button
            settingsButton.setOnClickListener(v -> {
                String settingsDeviceName = PreferencesManager.getInstance(this).getDeviceName(address);
                if(settingsDeviceName == null) settingsDeviceName = device.getName();
                Log.d(TAG, "Settings button clicked for device: " + settingsDeviceName + " (" + address + ")");
                Intent intent = new Intent(SelectDeviceActivity.this, DeviceItemSettings.class);
                intent.putExtra("deviceName", settingsDeviceName);
                intent.putExtra("deviceAddress", device.getAddress());
                deviceSettingsLauncher.launch(intent);
            });

            // Add the updated view to the container

            if(divider != null){
                if(index == totalDevices -1){
                    divider.setVisibility(View.GONE);
                } else {
                    divider.setVisibility(View.VISIBLE);
                }
            }
            deviceContainer.addView(deviceView);
            index++;
        }
    }
    private final ActivityResultLauncher<Intent> deviceSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Returning from DeviceSettingsActivity - refreshing UI");
                            loadDiscoveredDevices();
                            updateTheUI();
                        }
            });

    @SuppressLint("MissingPermission")
    private void saveDiscoveredDevices(){
        Log.d(TAG, "Saving discovered devices: " + deviceList.size());
        for(BluetoothDevice device : deviceList){
            String address = device.getAddress();
            String name = device.getName();
            boolean hasCharacteristic = deviceCharFlagMap.containsKey(address) && deviceCharFlagMap.get(address);
            String status = deviceStatusMap.containsKey(address) ? deviceStatusMap.get(address) : "Disconnected";
            Log.d(TAG, "Saving device: " + name + " (" + address + ")" + " with characteristic: " + hasCharacteristic + " and status: " + status);
            PreferencesManager.getInstance(this).saveDevice(address, name, hasCharacteristic, status);
        }
    }
    private void loadDiscoveredDevices(){

        deviceList.clear();
        Log.d(TAG, "Loading discovered devices");
        List<JSONObject> savedDevices = PreferencesManager.getInstance(this).loadDevices();

        for(JSONObject deviceObject : savedDevices){
            try{
                String address = deviceObject.getString("address");
                String name = deviceObject.getString("name");
                boolean hasCharacteristic = deviceObject.getBoolean("hasCharacteristic");
                String status = deviceObject.getString("status");

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                if(!deviceList.contains(device)) {
                    deviceList.add(device);
                    deviceCharFlagMap.put(address, hasCharacteristic);
                    currentlyConnectedDevice = ConnectedDeviceManager.getInstance().getDeviceAddress();
                    deviceStatusMap.put(address, status);
                }
            } catch (JSONException e){
                e.printStackTrace();
            }
            Log.d(TAG, "Loaded device: " + deviceObject.toString());
        }
    }

    private void refreshDeviceList() {
        Log.d(TAG, "Refreshing device list");
       PreferencesManager.getInstance(this).clearDevices();

        deviceList.clear();
        deviceSet.clear();
        Log.d(TAG, "Cleared device set and list");
        deviceCharFlagMap.clear();
        deviceStatusMap.clear();
        updateTheUI();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if(device == null){
            Log.e(TAG, "Device is null");
            return;
        }
        if(currentlyConnectedDevice != null){
            Log.e(TAG, "Already connected to a device disconnecting");
            disconnectFromDevice(bluetoothAdapter.getRemoteDevice(currentlyConnectedDevice));
        }
        if(device.getAddress() == null){
            Log.e(TAG, "Device address is null");
            return;
        }
        String address = device.getAddress();
        Log.d(TAG, "Connecting to " + device.getName());

        deviceStatusMap.put(device.getAddress(), "Connecting");
        PreferencesManager.getInstance(this).saveDevice(
                device.getAddress(),
               PreferencesManager.getInstance(this).getDeviceName(device.getAddress()),
                PreferencesManager.getInstance(this).getDeviceCharFlag(device.getAddress()),
                "Connecting"
        );
        updateTheUI();

        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("management_BLEService", "CONNECT");
        intent.putExtra("device_address", address);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        ConnectedDeviceManager.getInstance().setDeviceName(device.getName());
        ConnectedDeviceManager.getInstance().setDeviceAddress(device.getAddress());
        currentlyConnectedDevice = device.getAddress();
        Log.d(TAG, "currentlyConnectedDevice: " + currentlyConnectedDevice + " " + device.getName());
    }

    private final BroadcastReceiver isDeviceConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = intent.getBooleanExtra("isConnected", false);
            Log.d(TAG, "Received connection state: " + isConnected);


            String address = ConnectedDeviceManager.getInstance().getDeviceAddress();
            ConnectedDeviceManager.getInstance().setConnected(isConnected);
            boolean accurateDeviceStatus = ConnectedDeviceManager.getInstance().getDeviceStatus(address);
            Log.d(TAG, "Received address: " + address);
            Log.d(TAG, "Received connection state: " + ConnectedDeviceManager.getInstance().getDeviceStatus(address));

            if(address != null){
                String status = accurateDeviceStatus ? "Connected" : "Disconnected";
                Log.d("onReceive", "Saving device: " + address + " with status: " + status);
                PreferencesManager.getInstance(context).saveDevice(
                        address,
                        PreferencesManager.getInstance(context).getDeviceName(address),
                        PreferencesManager.getInstance(context).getDeviceCharFlag(address),
                        status
                );

                deviceStatusMap.put(address, status);
                Log.d(TAG, "deviceStatusMap: " + address + " " + status);
                currentlyConnectedDevice = isConnected ? address : null;
                Log.d(TAG, "currentlyConnectedDevice: " + currentlyConnectedDevice + " status " + status);
                Log.d(TAG, "Updated deviceStatusMap: " + address + " -> " + status);
            }
            updateTheUI();
        }
    };

    @SuppressLint("MissingPermission")
    private void disconnectFromDevice(BluetoothDevice device) {
        if(device == null){
            Log.e(TAG, "Device is null");
            return;
        }
        ConnectedDeviceManager.getInstance().setDeviceName(device.getName());
        Log.d(TAG, "Disconnecting from " + device.getName());
        Log.d(TAG, "Disconnecting from " + device.getAddress());
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("management_BLEService", "DISCONNECT");
        intent.putExtra("device_address", device.getAddress());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        currentlyConnectedDevice = null;
        deviceStatusMap.put(device.getAddress(), "Disconnected");
        updateTheUI();
    }
}
