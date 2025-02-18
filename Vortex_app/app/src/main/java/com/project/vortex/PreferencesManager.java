package com.project.vortex;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PreferencesManager {
    private static final String TAG = "PreferencesManager";
    private static final String PREF_NAME = "DiscoveredAndApprovedDevices";
    private static final String DEVICES_KEY = "savedDevicesArray";

    private static PreferencesManager instance;
    private final SharedPreferences sharedPreferences;

    private PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    // Save a device to SharedPreferences
    public void saveDevice(String address, String name, boolean hasCharacteristic, String status) {
        Log.d(TAG, "Saving device: " + name + " with address: " + address + " and status: " + status + " and hasCharacteristic: " + hasCharacteristic);
        try {
            String existingJson = sharedPreferences.getString(DEVICES_KEY, "[]");
            JSONArray devicesArray = new JSONArray(existingJson);

            // Check if the device already exists in the array
            for (int i = 0; i < devicesArray.length(); i++) {
                JSONObject deviceObject = devicesArray.getJSONObject(i);
                if (deviceObject.getString("address").equals(address)) {
                    // Update the existing device
                    deviceObject.put("name", name);
                    deviceObject.put("hasCharacteristic", hasCharacteristic);
                    deviceObject.put("status", status);
                    devicesArray.put(i, deviceObject);
                    saveDevicesArray(devicesArray);
                    return;
                }
            }

            // Add the new device if it doesn't exist
            JSONObject newDevice = new JSONObject();
            newDevice.put("address", address);
            newDevice.put("name", name);
            newDevice.put("hasCharacteristic", hasCharacteristic);
            newDevice.put("status", status);
            devicesArray.put(newDevice);
            saveDevicesArray(devicesArray);
        } catch (JSONException e) {
            Log.e(TAG, "Error saving device: " + e.getMessage());
        }
    }

    // Load all devices from SharedPreferences
    public List<JSONObject> loadDevices() {
        List<JSONObject> deviceList = new ArrayList<>();
        try {
            String existingJson = sharedPreferences.getString(DEVICES_KEY, "[]");
            JSONArray devicesArray = new JSONArray(existingJson);
            for (int i = 0; i < devicesArray.length(); i++) {
                deviceList.add(devicesArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading devices: " + e.getMessage());
        }
        return deviceList;
    }

    // Remove a specific device
    public void removeDevice(String address) {
        try {
            String existingJson = sharedPreferences.getString(DEVICES_KEY, "[]");
            JSONArray devicesArray = new JSONArray(existingJson);
            JSONArray updatedArray = new JSONArray();

            for (int i = 0; i < devicesArray.length(); i++) {
                JSONObject deviceObject = devicesArray.getJSONObject(i);
                if (!deviceObject.getString("address").equals(address)) {
                    updatedArray.put(deviceObject);
                }
            }
            saveDevicesArray(updatedArray);
        } catch (JSONException e) {
            Log.e(TAG, "Error removing device: " + e.getMessage());
        }
    }

    // Clear all saved devices
    public void clearDevices() {
        sharedPreferences.edit().remove(DEVICES_KEY).apply();
    }
    //Get device Status
    public String getDeviceStatus(String address) {
        try {
            String existingJson = sharedPreferences.getString(DEVICES_KEY, "[]");
            JSONArray devicesArray = new JSONArray(existingJson);
            for (int i = 0; i < devicesArray.length(); i++) {
                JSONObject deviceObject = devicesArray.getJSONObject(i);
                if (deviceObject.getString("address").equals(address)) {
                    return deviceObject.optString("status", "Disconnected");
                }
                }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting device status: " + e.getMessage());
        }
        return "Disconnected";
    }
    // Save the updated devices array
    private void saveDevicesArray(JSONArray devicesArray) {
        sharedPreferences.edit().putString(DEVICES_KEY, devicesArray.toString()).apply();
        Log.d(TAG, "Devices saved: " + devicesArray.toString());
    }
}
