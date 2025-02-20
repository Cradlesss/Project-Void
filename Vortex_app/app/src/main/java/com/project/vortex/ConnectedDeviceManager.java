package com.project.vortex;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class ConnectedDeviceManager {
    private static ConnectedDeviceManager instance;
    private String deviceName;
    private boolean isConnected;
    private String deviceAddress;
    private Map<String, Boolean> deviceCharFlagMap = new HashMap<>();
    private Map<String, Boolean> deviceStatusMap = new HashMap<>(); // To track device statuses>

    private ConnectedDeviceManager(){}

    public static ConnectedDeviceManager getInstance() {
        if (instance == null) {
            instance = new ConnectedDeviceManager();
        }
        return instance;
    }
    public void setDeviceName(String deviceName) {
        Log.d("ConnectedDeviceManager", "setDeviceName called with value: " + deviceName);
        this.deviceName = deviceName;
    }
    public void setConnected(boolean connected) {
        Log.d("ConnectedDeviceManager", "setConnected called with value: " + connected);
        this.isConnected = connected;
    }
    public void setDeviceAddress(String address) {
        Log.d("ConnectedDeviceManager", "setDeviceAddress called with value: " + address);
        this.deviceAddress = address;
    }
    public String getDeviceAddress() {
        return deviceAddress;
    }
    public boolean isConnected() {
        return isConnected;
    }
    public String getDeviceName() {
        return deviceName;
    }
    public boolean getDeviceCharFlag(String deviceAddress) {
        Log.d("ConnectedDeviceManager", "getDeviceCharFlag called with deviceAddress: " + deviceAddress);
        return deviceCharFlagMap.getOrDefault(deviceAddress, false);
    }
    public void setDeviceCharFlag(String deviceAddress, boolean flag) {
        Log.d("ConnectedDeviceManager", "setDeviceCharFlag called with deviceAddress: " + deviceAddress + ", flag: " + flag);
        deviceCharFlagMap.put(deviceAddress, flag);
    }
    public Map<String, Boolean> getDeviceCharFlagMap() {
        Log.d("ConnectedDeviceManager", "getDeviceCharFlagMap called");
        return deviceCharFlagMap;
    }
    public void setDeviceStatus(String deviceAddress, boolean status) {
        Log.d("ConnectedDeviceManager", "setDeviceStatus called with deviceAddress: " + deviceAddress + ", status: " + status);
        deviceStatusMap.put(deviceAddress, status);
    }
    public boolean getDeviceStatus(String deviceAddress) {
        Log.d("ConnectedDeviceManager", "getDeviceStatus called with deviceAddress: " + deviceAddress);
        return deviceStatusMap.getOrDefault(deviceAddress, false);
    }
    public Map<String, Boolean> getDeviceStatusMap() {
        Log.d("ConnectedDeviceManager", "getDeviceStatusMap called");
        return deviceStatusMap;
    }
}
