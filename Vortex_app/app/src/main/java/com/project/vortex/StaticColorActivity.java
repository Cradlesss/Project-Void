package com.project.vortex;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

public class StaticColorActivity extends AppCompatActivity {
    private static final String TAG = "StaticColorActivity";

    private View colorPreview;
    private Button savedColor1, savedColor2, savedColor3;
    private Button applyColorButton;
    private ImageButton backButton;
    private FrameLayout backButtonFrame;
    private EditText hexInput;
    private ColorPickerView colorPickerView;
    private TextView rValue, gValue, bValue;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_static_color);
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Initial connection status: " + isConnected);

        colorPreview = findViewById(R.id.color_preview);
        applyColorButton = findViewById(R.id.apply_color_button);
        savedColor1 = findViewById(R.id.saved_color_1);
        savedColor2 = findViewById(R.id.saved_color_2);
        savedColor3 = findViewById(R.id.saved_color_3);
        hexInput = findViewById(R.id.hex_input);
        backButton = findViewById(R.id.back_button);
        backButtonFrame = findViewById(R.id.back_button_frame);
        colorPickerView = findViewById(R.id.colorPickerView);
        rValue = findViewById(R.id.r_value);
        gValue = findViewById(R.id.g_value);
        bValue = findViewById(R.id.b_value);
        RelativeLayout root = findViewById(R.id.static_color_activity);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            root.setPadding(
                    i.left,
                    i.top,
                    i.right,
                    v.getPaddingBottom());
            return insets;
        });

        setupColorWheel();
        setupHexInputWatcher();

        // Load saved colors
        loadColorFromPreference(savedColor1, "static_color_saved_1");
        loadColorFromPreference(savedColor2, "static_color_saved_2");
        loadColorFromPreference(savedColor3, "static_color_saved_3");

        // Saved Colors Listeners
        savedColor1.setOnLongClickListener(v -> saveColorToButton(savedColor1, "static_color_saved_1"));
        savedColor2.setOnLongClickListener(v -> saveColorToButton(savedColor2, "static_color_saved_2"));
        savedColor3.setOnLongClickListener(v -> saveColorToButton(savedColor3, "static_color_saved_3"));

        savedColor1.setOnClickListener(v -> applySavedColor(savedColor1));
        savedColor2.setOnClickListener(v -> applySavedColor(savedColor2));
        savedColor3.setOnClickListener(v -> applySavedColor(savedColor3));

        // Back Button Listener
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
        backButton.setOnClickListener(sharedBackListener);
        backButtonFrame.setOnClickListener(sharedBackListener);

        // Bottom Navigation Setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.static_color) {
                return true;
            } else if (itemId == R.id.animations) {
                Intent intent = new Intent(this, Animations.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.settings) {
                Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
                return false;
            }
            return false;
        });

        applyColorButton.setOnClickListener(v -> handleCommand());
    }
    private void handleCommand() {
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Connection state updated in handleCommand: " + isConnected);

        if(isConnected){
            applyColorButton.setEnabled(false);
            sendSelectedColor();
            new Handler().postDelayed(() -> applyColorButton.setEnabled(true), 1000);
        } else {
            Toast.makeText(this, R.string.handleCommand, Toast.LENGTH_SHORT).show();
            applyColorButton.setEnabled(true);
        }
    }

    private void setupColorWheel(){
        colorPickerView.setColorListener(new ColorEnvelopeListener() {
            @Override
            public void onColorSelected(ColorEnvelope colorEnvelope, boolean fromUser) {
                int selectedColor = colorEnvelope.getColor();
                int r = Color.red(selectedColor);
                int g = Color.green(selectedColor);
                int b = Color.blue(selectedColor);

                rValue.setText("R: " + r);
                gValue.setText("G: " + g);
                bValue.setText("B: " + b);

                String hexColor = String.format("#%06X", (0xFFFFFF & selectedColor));

                hexInput.setText(hexColor);
                updateColorPreview(hexColor);
            }
        });
    }

    private void setupHexInputWatcher() {
        TextView errorMessage = findViewById(R.id.error_message);
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String hex = s.toString();

                if(!hex.startsWith("#")){
                    hexInput.setText("#" + hex.replace("#", ""));
                    hexInput.setSelection(hexInput.getText().length());
                    return;
                }

                if (isValidHexColor(hex)) {
                    errorMessage.setVisibility(View.GONE);
                    updateColorPreview(hex);
                } else {
                    errorMessage.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String hex = s.toString();
                if(isValidHexColor(hex) || hex.equals("#")){
                    errorMessage.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateColorPreview(String hexColor) {
        try {
            int color = Color.parseColor(hexColor);
            GradientDrawable drawable = (GradientDrawable) colorPreview.getBackground();
            drawable.setColor(color);
            hexInput.setTextColor(Color.BLACK);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid color: " + hexColor);
        }
    }

    private boolean isValidHexColor(String hex) {
        return hex.matches("^#[0-9A-Fa-f]{6}$");
    }

    private boolean saveColorToButton(Button button, String key) {
        String hex = hexInput.getText().toString();
        if (isValidHexColor(hex)) {
            int color = Color.parseColor(hex);

            GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.rounded_background).mutate();
            drawable.setColor(color);
            button.setBackground(drawable);
            button.setTag(hex); // Save the color as a tag
            saveColorToPreference(key, hex);
            Toast.makeText(this, R.string.color_saved, Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(this, R.string.invalid_hex_color, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void loadColorFromPreference(Button button, String key) {
        SharedPreferences preferences = getSharedPreferences("SavedColors", MODE_PRIVATE);
        String savedHex = preferences.getString(key, null);

        if(savedHex != null){
            int color = Color.parseColor(savedHex);

            GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.rounded_background).mutate();
            drawable.setColor(color);
            button.setBackground(drawable);
            button.setTag(savedHex); // Save the color as a tag
        }
    }

    private void saveColorToPreference(String key, String hexColor){
        getSharedPreferences("SavedColors", MODE_PRIVATE)
                .edit()
                .putString(key, hexColor)
                .apply();
    }

    private void applySavedColor(Button button) {
        String savedHex = (String) button.getTag(); // Retrieve saved color from tag
        if (savedHex != null) {
            hexInput.setText(savedHex); // Set hex input to saved color
            updateColorPreview(savedHex); // Update the color preview
            Log.d(TAG, "Color applied: " + savedHex);
        } else {
            Toast.makeText(this, R.string.no_color_saved, Toast.LENGTH_SHORT).show();
        }
    }


    private void sendSelectedColor() {
        String hex = hexInput.getText().toString();
        if (!isValidHexColor(hex)) {
            Toast.makeText(this, R.string.invalid_hex_color, Toast.LENGTH_SHORT).show();
            return;
        }

        int red = Color.red(Color.parseColor(hex));
        int green = Color.green(Color.parseColor(hex));
        int blue = Color.blue(Color.parseColor(hex));

        Log.d(TAG, "Sending color: R=" + red + ", G=" + green + ", B=" + blue);

        Intent indicatorCommand = new Intent(this, BLEService.class);
        indicatorCommand.putExtra("management_BLEService", "DRIVER_COMMAND");
        indicatorCommand.putExtra("command", 13);
        startService(indicatorCommand);

        new Handler().postDelayed(() -> {
            Intent redCommand = new Intent(this, BLEService.class);
            redCommand.putExtra("management_BLEService", "DRIVER_COMMAND");
            redCommand.putExtra("command", red);
            startService(redCommand);
        }, 250);

        new Handler().postDelayed(() -> {
            Intent greenCommand = new Intent(this, BLEService.class);
            greenCommand.putExtra("management_BLEService", "DRIVER_COMMAND");
            greenCommand.putExtra("command", green);
            startService(greenCommand);
        }, 500);

        new Handler().postDelayed(() -> {
            Intent blueCommand = new Intent(this, BLEService.class);
            blueCommand.putExtra("management_BLEService", "DRIVER_COMMAND");
            blueCommand.putExtra("command", blue);
            startService(blueCommand);
        }, 750);

        Toast.makeText(this, R.string.color_applied, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Connection state updated in onResume: " + isConnected);
        updateUIBasedOnConnectionState();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.static_color);

        // Load saved colors
        loadColorFromPreference(savedColor1, "static_color_saved_1");
        loadColorFromPreference(savedColor2, "static_color_saved_2");
        loadColorFromPreference(savedColor3, "static_color_saved_3");

        EditText editText = findViewById(R.id.hex_input);
        editText.setBackgroundColor(Color.WHITE);
        editText.setBackgroundResource(R.drawable.rounded_background);
        String hex = editText.getText().toString();
        if (isValidHexColor(hex)) {
            updateColorPreview(hex);
        } else {
            resetColorPreview();
        }
    }
    private void updateUIBasedOnConnectionState() {
        if (!isConnected) {
            Toast.makeText(this, R.string.updateUIBasedOnConnectionState, Toast.LENGTH_SHORT).show();
        }
    }
    private void resetColorPreview() {
        try {
            GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.rounded_background).mutate();
            drawable.setColor(Color.WHITE); // Default color (white)
            colorPreview.setBackground(drawable);
        } catch (Exception e) {
            Log.e(TAG, "Error resetting colorPreview: " + e.getMessage());
        }
    }
}
