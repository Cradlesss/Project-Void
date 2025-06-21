package com.project.vortex;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
///Scraped for now

public class TestingNewActivities extends AppCompatActivity {

    private Button openAnimationsButton, staticColorButton;
    private ImageButton selectDeviceButton;
    private FrameLayout selectDeviceFrame;
    private SeekBar brightnessSeekBar;
    private TextView statusText;
    private BottomNavigationView bottomNavigationView;
    private int currentAnimation = 0;
    private LEDStripPreviewView ledPreview;
    private String status;
    private String deviceName;
    private boolean isConnected = false;
    private static final String TAG = "TestingNewActivities";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing_new_activities);
        LocalBroadcastManager.getInstance(this).registerReceiver(bleStatusReceiver,
                new IntentFilter("BLEServiceStatusUpdate"));
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        deviceName = ConnectedDeviceManager.getInstance().getDeviceName();

        registerUI();

        openAnimationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Animations.class);
            startActivity(intent);
        });
        staticColorButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StaticColorActivity.class);
            startActivity(intent);
        });

        ledPreview.setActiveAnimation(currentAnimation);
        bottomNavigationView.setSelectedItemId(R.id.settings);

        EditText editText = findViewById(R.id.animationHashtag);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence != null && charSequence.length() > 0){
                int input = Integer.parseInt(charSequence.toString());
                Log.d(TAG, "Input: " + input);
                updateLEDPreview(input);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

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
                return true;
            } else if(itemId == R.id.static_color){
                Intent intent = new Intent(this, StaticColorActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        View.OnClickListener sharedBackListener = v -> {
            Log.d(TAG, "Select device button clicked");
            int centerX = selectDeviceFrame.getWidth() / 2;
            int centerY = selectDeviceFrame.getHeight() / 2;
            selectDeviceFrame.setPressed(true);
            Drawable foreground = selectDeviceFrame.getForeground();
            if(foreground instanceof RippleDrawable){
                (foreground).setHotspot(centerX, centerY);
            }
            selectDeviceFrame.postDelayed(() -> {
                selectDeviceFrame.setPressed(false);
                Intent intent = new Intent(this, SelectDeviceActivity.class);
                startActivity(intent);
            }, 200);
        };
        selectDeviceButton.setOnClickListener(sharedBackListener);
        selectDeviceFrame.setOnClickListener(sharedBackListener);

        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "Progress stopped");
            }
        });
    }

    private void updateLEDPreview(int newAnimation) {
        currentAnimation = newAnimation;
        ledPreview.setActiveAnimation(currentAnimation);
    }


    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.settings);

        LocalBroadcastManager.getInstance(this).registerReceiver(bleStatusReceiver,
                new IntentFilter("BLEServiceStatusUpdate"));
        deviceName = PreferencesManager.getInstance(this).getDeviceName(ConnectedDeviceManager.getInstance().getDeviceAddress());
        if(deviceName == null) deviceName = ConnectedDeviceManager.getInstance().getDeviceName();
        isConnected = ConnectedDeviceManager.getInstance().isConnected();

        if(isConnected){
            statusText.setText("Connected to: " + deviceName);
        } else {
            statusText.setText(R.string.default_statusText);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bleStatusReceiver);
    }
    private void registerUI() {
        selectDeviceButton = findViewById(R.id.selectDeviceBtn);
        openAnimationsButton = findViewById(R.id.goToAnimations);
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.home);
        staticColorButton = findViewById(R.id.staticColorBtn);
        statusText = findViewById(R.id.statusText);
        statusText.setText(R.string.default_statusText);
        selectDeviceFrame = findViewById(R.id.selectDeviceFrame);
        selectDeviceButton = findViewById(R.id.selectDeviceBtn);
        ledPreview = findViewById(R.id.ledStripPreview);
    }
    private BroadcastReceiver bleStatusReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            if (intent.hasExtra("status")) {
                status = intent.getStringExtra("status");
                statusText.setText(status);
            }
        }
    };
}
