package com.project.vortex;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.UUID;

public class BLEService extends Service {

    private static final String TAG = "BLEService";
    private static final long COMMAND_DELAY = 100; // Xms delay between commands
    private long lastCommandTime = 0;
    private long lastCommand12Time = 0;
    private static final long COMMAND_12_DELAY = 100; // Xms delay between 12 commands
    private static final String CHANNEL_ID = "BLEServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    // BLE
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic animationCharacteristic;
    private BluetoothGattCharacteristic notSupportedCharacteristic;
    private static boolean Connected = false;

    // UUIDs
    private static final UUID SERVICE_UUID = UUID.fromString("895dc926-817a-424d-8736-f582d2dbac8e"); // Replace with your service UUID
    private static final UUID LEDS_UUID = UUID.fromString("7953deb4-b2e1-4829-a692-8ec173cc71fc"); // Replace with your characteristic UUID
    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createNotificationChannel();

        Connected = false;
        startForegroundServiceWithCompatibility();
        Log.d(TAG, "BLEService created");
    }

    private void startForegroundServiceWithCompatibility() {
        Notification notification = createNotification("BLE Service is running...");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("management_BLEService")) {
            String management = intent.getStringExtra("management_BLEService");
            if ("DISCONNECT".equals(management) && intent.hasExtra("device_address")) {
                String deviceAddress = intent.getStringExtra("device_address");
                disconnectFromDevice(deviceAddress);
            } else if("CONNECT".equals(management) && intent.hasExtra("device_address")){
                String deviceAddress = intent.getStringExtra("device_address");
                Log.d(TAG, "Connecting to device: " + deviceAddress);
                connectToDevice(deviceAddress);
            } else if("DRIVER_COMMAND".equals(management) && intent.hasExtra("command")){
                int command = intent.getIntExtra("command", -1);
                sendCommand(command);
            }
        }
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        isDeviceConnected(false);
        Connected = false;
        PreferencesManager.getInstance(this).saveDevice(
                ConnectedDeviceManager.getInstance().getDeviceAddress(),
                ConnectedDeviceManager.getInstance().getDeviceName(),
                ConnectedDeviceManager.getInstance().getDeviceCharFlag(
                        ConnectedDeviceManager.getInstance().getDeviceAddress()
                ),
                "Disconnected"
        );
        ConnectedDeviceManager.getInstance().setConnected(false);
        ConnectedDeviceManager.getInstance().setDeviceAddress(null);

        Log.d(TAG, "BLEService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private boolean hasBluetoothPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void disconnectFromDevice(String deviceAddress) {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Permission not granted");
            return;
        }
        if(deviceAddress != null){
            PreferencesManager.getInstance(this).saveDevice(
                    deviceAddress,
                    ConnectedDeviceManager.getInstance().getDeviceName(),
                    true,
                    "Disconnected"
            );
        }
        Log.d(TAG, "Trying to disconnect from device: " + deviceAddress);
        if (bluetoothGatt == null) {
            Log.e(TAG, "No active connection found");
            return;
        }
        if(bluetoothGatt != null && bluetoothGatt.getDevice().getAddress().equals(deviceAddress)){
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            Log.d(TAG, "Disconnected from device");
            isDeviceConnected(false);
            ConnectedDeviceManager.getInstance().setConnected(false);
            ConnectedDeviceManager.getInstance().setDeviceAddress(null);
            sendStatusUpdate("Disconnected from device");
            updateNotification("Disconnected from device");
            Connected = false;
        } else {
            Log.e(TAG, "No active connection found for address " + deviceAddress + bluetoothGatt.getDevice().getName());
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(String deviceAddress) {
        Log.d(TAG, "Trying to connect to device: " + deviceAddress);
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Permission not granted");
            return;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            Log.e(TAG, "Device not found");
            return;
        }
        if (bluetoothGatt != null){
            if (bluetoothGatt.getDevice().getAddress().equals(deviceAddress)) {
                Log.d(TAG, "Already connected to device: " + deviceAddress);
                isDeviceConnected(true);
                Connected = true;
                sendStatusUpdate("Connected to " + device.getName());
                updateNotification("Connected to " + device.getName());
                ConnectedDeviceManager.getInstance().setDeviceName(device.getName());
                ConnectedDeviceManager.getInstance().setDeviceAddress(deviceAddress);
                ConnectedDeviceManager.getInstance().setConnected(true);
                PreferencesManager.getInstance(this).saveDevice(
                        deviceAddress,
                        ConnectedDeviceManager.getInstance().getDeviceName(),
                        ConnectedDeviceManager.getInstance().getDeviceCharFlag(deviceAddress),
                        "Connected"
                );
                return;
            } else {
                Log.d(TAG, "Disconnecting from previous device");
                isDeviceConnected(false);
                Connected = false;
                ConnectedDeviceManager.getInstance().setConnected(false);
                ConnectedDeviceManager.getInstance().setDeviceAddress(null);
                PreferencesManager.getInstance(this).saveDevice(
                        deviceAddress,
                        ConnectedDeviceManager.getInstance().getDeviceName(),
                        true,
                        "Disconnected"
                );
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
        }
        Log.d(TAG, "Connecting to device: " + device.getName() + " (" + deviceAddress + ")");
        bluetoothGatt = device.connectGatt(this, false, gattCallback); // autoConnect = false for initial connection

        if (bluetoothGatt == null) {
            Log.e(TAG, "Failed to connect to device");
            return;
        }


        ConnectedDeviceManager.getInstance().setDeviceName(device.getName());
        updateNotification("Connecting to " + device.getName());
        sendStatusUpdate("Connecting to " + device.getName());

        // Timeout if connection doesn't succeed
        handler.postDelayed(() -> {
            if (bluetoothGatt != null && animationCharacteristic == null) {
                Log.e(TAG, "Connection timed out");
                PreferencesManager.getInstance(this).saveDevice(
                        ConnectedDeviceManager.getInstance().getDeviceAddress(),
                        ConnectedDeviceManager.getInstance().getDeviceName(),
                        ConnectedDeviceManager.getInstance().getDeviceCharFlag(
                                ConnectedDeviceManager.getInstance().getDeviceAddress()
                        ),
                        "Disconnected"
                );
                ConnectedDeviceManager.getInstance().setDeviceAddress(null);
                ConnectedDeviceManager.getInstance().setDeviceName(null);
                ConnectedDeviceManager.getInstance().setConnected(false);
                isDeviceConnected(false);
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        }, 10000); // X-second timeout
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server");
                    gatt.discoverServices();
                    ConnectedDeviceManager.getInstance().setDeviceAddress(gatt.getDevice().getAddress());
                    updateNotification("Connected to " + gatt.getDevice().getName());
                    isDeviceConnected(true);
                    Connected = true;
                    sendStatusUpdate("Connected to " + gatt.getDevice().getName());
                    ConnectedDeviceManager.getInstance().setDeviceName(gatt.getDevice().getName());
                    ConnectedDeviceManager.getInstance().setConnected(true);
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server");
                    updateNotification("Disconnected from " + gatt.getDevice().getName());
                    sendStatusUpdate("Disconnected from " + gatt.getDevice().getName());
                    isDeviceConnected(false);
                    Connected = false;
                    reconnectToDevice(gatt.getDevice());
                }
            } else {
                Log.e(TAG, "Connection failed with status: " + status);
                PreferencesManager.getInstance(BLEService.this).saveDevice(
                        ConnectedDeviceManager.getInstance().getDeviceAddress(),
                        ConnectedDeviceManager.getInstance().getDeviceName(),
                        ConnectedDeviceManager.getInstance().getDeviceCharFlag(
                                ConnectedDeviceManager.getInstance().getDeviceAddress() // sometimes sets value to false?
                        ),
                        "Disconnected"
                );
                isDeviceConnected(false);
                Connected = false;
                sendStatusUpdate("Connect to a device");
                updateNotification("Connect to a device");
                ConnectedDeviceManager.getInstance().setConnected(false);
                ConnectedDeviceManager.getInstance().setDeviceAddress(null);
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    Log.d(TAG, "Service found with UUID: " + service.getUuid());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().equals(LEDS_UUID)) {
                            animationCharacteristic = characteristic;
                            Log.d("ANIMATION", "Writable characteristic found: " + animationCharacteristic.getUuid());
                            break; // Exit loop once the characteristic is found
                        } else if (!characteristic.getUuid().equals(LEDS_UUID)) {
                            Log.e("ANIMATION", "Not supported characteristic found: " + characteristic.getUuid());
                            notSupportedCharacteristic = characteristic;
                            break;
                        }
                    }
                    if (animationCharacteristic == null) {
                        Log.e(TAG, "No writable characteristic found with UUID: " + LEDS_UUID);
                    }
                } else {
                    Log.e(TAG, "Service not found with UUID: " + SERVICE_UUID);
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Command sent successfully");
            } else {
                Log.e(TAG, "Failed to send command with status: " + status);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void reconnectToDevice(BluetoothDevice device) {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Permission not granted");
            return;
        }
        if (device != null) {
            handler.postDelayed(() -> {
                Log.d(TAG, "Reconnecting to device: " + device.getName());
                bluetoothGatt = device.connectGatt(this, true, gattCallback); // autoConnect = true for reconnection
            }, 5000); // 5-second delay before reconnecting
        } else {
            Log.e(TAG, "Device is null. Cannot reconnect.");
        }
    }

    @SuppressLint("MissingPermission")
    private void sendCommand(int command) {
        long currentTime = System.currentTimeMillis();

        if ((command == 12 && (currentTime - lastCommand12Time) >= COMMAND_12_DELAY) ||
                (command != 12 && (currentTime - lastCommandTime) >= COMMAND_DELAY)) {
            if (command == 12) {
                lastCommand12Time = currentTime; // only for 12
            } else {
                lastCommandTime = currentTime;
            }

            if (!hasBluetoothPermission()) {
                Log.e(TAG, "Permission not granted");
                return;
            }
            if (animationCharacteristic != null) {
                animationCharacteristic.setValue(new byte[]{(byte) command});
                boolean success = bluetoothGatt.writeCharacteristic(animationCharacteristic);
                if (success) {
                    Log.d(TAG, "Command sent: " + command);
                } else {
                    Log.e(TAG, "Failed to send command: " + command);
                }
            } else {
                Log.e(TAG, "Writable characteristic is null. Cannot send command.");
            }
        } else {
            Log.d(TAG, "Command: " + command + " not sent. Delay not reached.");
        }
    }
    private Notification createNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BLE Service")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String contentText) {
        Notification notification = createNotification(contentText);
        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void sendStatusUpdate(String status){
        Intent intent = new Intent("BLEServiceStatusUpdate");
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Status sent: " + status);
    }

    public void isDeviceConnected(boolean isConnected) {
        ConnectedDeviceManager.getInstance().setConnected(isConnected);
        Log.d(TAG, "Connection state updated: " + isConnected);

        Intent intent = new Intent("isDeviceConnected");
        intent.putExtra("isConnected", isConnected);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static boolean getIsConnected(){
        return Connected;
    }
}
/*
TODO:
   1. update the ui when device connected (done) ~ the UI in ActivityMain doesn't update when back from ex animations (might be done (check for further errors)) (done)
   2. notify user when device connected (done)
   3. detect when device disconnects and notify user (done)
   4. better UI (done)
   5. selecting static colors not only animations (done)
   6. Ideas for more features?
   7. Fix the laggy brightness and make it replay last sent animation or static color after user finished input
   8. Disconnect button (done)
   9. Update UI, Notification and StatusText when connection crashes (done)
   10. Brightness accidentally sending bluetoothValue commands instead of brightness commands (done)
   11. Disable buttons in StaticActivity when disconnected from device (done)
   12. Rework UI in activity_select_device.xml (done)
   13. Validate HEX input while user is typing not while sending (done)
   14. new Icon Design & better app name (Project Void) ~ best one so far, (Project Synthwave) (VortexLink) (Project GhostLink)
   15. buttons can be also set on timeout (to properly send commands) (done)
   16. Fix updating deviceCharFlagMap in other activities (done)
   17. check if app can connect to 2 devices at a time and what happens if yes (make the app disconnect from last connected device) (done)
   18. Fix paired devices in selectDeviceActivity (done)
 */