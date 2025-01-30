package com.project.vortex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DeviceConnectionReceiver extends BroadcastReceiver {
     private static final String TAG = "ConnectionReceiver";

     @Override
    public void onReceive(Context context, Intent intent){
         Log.d(TAG, "Received broadcast");
         if(intent != null){
             boolean isConnected = intent.getBooleanExtra("isConnected", false);
             Log.d(TAG, "Received connection state: " + isConnected);

             ConnectedDeviceManager.getInstance().setConnected(isConnected);
         }
     }
}
