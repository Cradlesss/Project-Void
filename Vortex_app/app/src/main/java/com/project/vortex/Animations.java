package com.project.vortex;

import android.animation.AnimatorSet;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Arrays;
import java.util.List;

public class Animations extends AppCompatActivity {
    private static final String TAG = "Animations";
    private boolean isConnected = false; // Tracks connection state
    private  AnimationButtonAdapter adapter;
    private CustomSnapHelper snapHelper;
    BottomNavigationView bottomNavigationView;
    ImageButton backButton;
    FrameLayout backButtonFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.animations);

        // Initialize connection state from ConnectionStateManager
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Initial connection status: " + isConnected);

        // Initialize UI
        InitializeUI();

        // Set up back button
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

        // Navigation bar setup
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.animations) {
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
        AnimatorSet animatorSet = new AnimatorSet();
        if(animatorSet.isRunning()){
            Log.d(TAG, "AnimatorSet is running");
            animatorSet.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the connection state whenever the activity is resumed
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        Log.d(TAG, "Connection state updated in onResume: " + isConnected);

        // Update UI based on the connection state (optional)
        updateUIBasedOnConnectionState(isConnected);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.animations);
    }

    private void updateUIBasedOnConnectionState(boolean isConnected) {
        if (!isConnected) {
            Toast.makeText(this, R.string.updateUIBasedOnConnectionState, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCommand(int command) {
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("management_BLEService", "DRIVER_COMMAND");
        intent.putExtra("command", command);
        startService(intent); // Send command via BLE service
    }

    private void InitializeUI() {
        RecyclerView recyclerView = findViewById(R.id.recycler);
        List<AnimationItem> animationItems = Arrays.asList(
                new AnimationItem("Off", 0),
                new AnimationItem("Twinkle", 1),
                new AnimationItem("Jinx", 2),
                new AnimationItem("Transition Break", 3),
                new AnimationItem("Blueb", 4),
                new AnimationItem("Rainbow", 5),
                new AnimationItem("Beatr", 6),
                new AnimationItem("Run Red", 7),
                new AnimationItem("Raw Noise", 8),
                new AnimationItem("Moving Rainbow", 9),
                new AnimationItem("Wave", 10),
                new AnimationItem("Blightb", 11)
        );

        adapter = new AnimationButtonAdapter(animationItems, (command, clickedButton) -> {
            handleCommand(command, clickedButton);
            Toast.makeText(Animations.this, "Button clicked: " + animationItems.get(command).getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
        );
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        int offset = getResources().getDimensionPixelOffset(R.dimen.selection_offset);
        snapHelper = new CustomSnapHelper(offset);
        snapHelper.attachToRecyclerView(recyclerView);

        View selectionOverlay = findViewById(R.id.selection_overlay);

        selectionOverlay.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                selectionOverlay.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int[] overlayLocation = new int[2];
                selectionOverlay.getLocationOnScreen(overlayLocation);
                int overlayCenterY = overlayLocation[1] + selectionOverlay.getHeight() / 2;

                int[] recyclerLocation = new int[2];
                recyclerView.getLocationOnScreen(recyclerLocation);
                int recyclerCenterY = recyclerLocation[1] + recyclerView.getHeight() / 2;
                int offset = recyclerCenterY - overlayCenterY;
                Log.d("OffsetCalc","Offset: " + offset);

                recyclerView.getViewTreeObserver().addOnPreDrawListener(() -> {
                    updateChildTransforms(recyclerView, offset);
                    return true;
                });
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapView = snapHelper.findSnapView(recyclerView.getLayoutManager());
                    if (snapView != null && recyclerView.getChildAdapterPosition(snapView) != RecyclerView.NO_POSITION) {
                        int position = layoutManager.getPosition(snapView);
                        Log.d(TAG, "Snapped to position: " + position);
                        adapter.setSelectedPosition(position);
                    }
                }
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.animations);
        backButton = findViewById(R.id.back_button);
        backButtonFrame = findViewById(R.id.back_button_frame);
    }

    private void updateChildTransforms(RecyclerView recyclerView, int offset) {
        // Instead of using recyclerView.getHeight()/2, adjust by subtracting the offset.
        int effectiveCenterY = recyclerView.getHeight() / 2 - offset;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            int childTop = child.getTop();
            int childBottom = child.getBottom();
            int childCenterY = (childTop + childBottom) / 2;
            float distanceFromCenter = Math.abs(effectiveCenterY - childCenterY);
            float normalized = Math.min(distanceFromCenter / (float) effectiveCenterY, 1f);
            // Scale: from 1.0 at the effective center down to 0.7 at the edges
            float scaleFactor = 1.0f - 0.3f * normalized;
            child.setScaleX(scaleFactor);
            child.setScaleY(scaleFactor);
            // Alpha: from 1.0 at the center to 0.5 at the edges
            float alpha = 1.0f - 0.5f * normalized;
            child.setAlpha(alpha);
        }
    }

    private void handleCommand(int command, Button clickedButton) {
        isConnected = ConnectedDeviceManager.getInstance().isConnected();
        if (isConnected) {
            clickedButton.setEnabled(false);
            sendCommand(command);
            new Handler().postDelayed(() -> clickedButton.setEnabled(true), 250);
        } else {
            Toast.makeText(this, R.string.handleCommand, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Paused Animations activity");
    }

    private void disabledButtons() {
        Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show();
    }
}
