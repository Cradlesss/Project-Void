package com.project.vortex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.Random;
/// Scraped for now
public class LEDStripPreviewView extends View {
    private int numLEDs = 10; // Number of simulated LEDs
    private int[] ledColors;   // Colors for each LED as Android color ints
    private Paint ledPaint;
    private int activeAnimation = 0; // Animation code: 0-11, or 13 for static color
    private Handler animationHandler = new Handler();
    private Runnable animationRunnable;
    private long updateInterval = 50; // Update every 50ms
    private Random random = new Random();

    //Twinkle variables
    private float[] ledBrightnessLevels;
    private boolean[] isTwinkling;
    private final int[] twinklePalette = new int[]{
            Color.RED,
            Color.rgb(255, 127, 0),  // orange
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.MAGENTA
    };

    //Nebula variables
    private float breathingProgress = 0f;
    private float breathingSpeed = 0.105f;

    //Transition with break variables
    private int transitionFrameCounter = 0;
    private double framesPerMove = 1.3;

    private int movingIndex = 0;


    // Variables for moving animations:
    private boolean movingForward = true;

    // For SET_STATIC_COLOR preview (if used)
    private int staticColor = Color.BLACK;

    public LEDStripPreviewView(Context context) {
        super(context);
        init();
    }

    public LEDStripPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ledColors = new int[numLEDs];
        ledBrightnessLevels = new float[numLEDs];
        isTwinkling = new boolean[numLEDs];
        Arrays.fill(ledColors, Color.BLACK);
        ledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ledPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Set the active animation code.
     * @param animation Animation code (0-11, or 13 for static color)
     */
    public void setActiveAnimation(int animation) {
        this.activeAnimation = animation;
        if(animation != 13) {
            startAnimationLoop();
        }
    }

    /**
     * For SET_STATIC_COLOR, set the static color.
     */
    public void setActiveAnimation(int animation, int staticColor) {
        this.activeAnimation = animation;
        if (animation == 13) {
            this.staticColor = staticColor;
            // Immediately fill with static color
            Arrays.fill(ledColors, staticColor);
            invalidate();
        } else {
            startAnimationLoop();
        }
    }

    private void startAnimationLoop() {
        // Remove any previous runnable
        if (animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                updateAnimation();
                invalidate();
                animationHandler.postDelayed(this, updateInterval);
            }
        };
        animationHandler.post(animationRunnable);
    }

    /**
     * Update the LED colors based on the active animation.
     * This is a simplified simulation of your FastLED animations.
     */
    private void updateAnimation() {
        switch (activeAnimation) {
            case 0: // OFF
                Arrays.fill(ledColors, Color.BLACK);
                break;
            case 1:// TWINKLE
                updateTwinkle();
                break;
            case 2: // NEBULA
                updateNebulaSurge();// Dark Surge a little brighter
                break;
            case 3: // TRANSITION_WITH_BREAK:
                updateTransitionWithBreak(); //background Color brighter, purple move maybe darker?
                break;
            case 4: // BLUEB:// Darker
                // Leave a fading trail by fading all LEDs
                updateBlueB(); //some dots on the sine wave updating incorrectly
                break;
            case 5: // RAINBOW: //redo whole thing
                // Use a sine function to oscillate the shift value
                float shift = (float)(Math.sin(System.currentTimeMillis() * 0.005) * 180 + 180);
                for (int i = 0; i < numLEDs; i++) {
                    float hue = (((float)i / numLEDs) * 360f + shift) % 360f;
                    ledColors[i] = Color.HSVToColor(new float[]{hue, 1f, 1f});
                }
                break;
            case 6: // BEATR (BlurPhaseBeat) //skip until arduino fixed? (overall broken
                // Create a repeating pattern: Green, Blue, Red at different positions.
                for (int i = 0; i < numLEDs; i++) {
                    int mod = i % 3;
                    if (mod == 0) {
                        ledColors[i] = Color.GREEN;
                    } else if (mod == 1) {
                        ledColors[i] = Color.BLUE;
                    } else {
                        ledColors[i] = Color.RED;
                    }
                    // Optionally, blur by fading a bit:
                    ledColors[i] = fadeColor(ledColors[i], 0.95f);
                }
                break;
            case 7: // RUNRED background brighter- moving dot maybe longer tail and def brighter red
            {
                // Define base colors:
                int darkRed = Color.rgb(50, 0, 0); // lighter
                int brightRed = Color.rgb(255, 0, 0); // darker
                // Set the entire strip to dark red.
                Arrays.fill(ledColors, darkRed);

                // Calculate the moving pixel's position (moving from right to left)
                int pos = numLEDs - movingIndex - 1;

                // Set the moving pixel to bright red.
                ledColors[pos] = brightRed;

                // Set the tail behind the moving pixel.
                // "Behind" means at a higher index than the moving pixel.
                if (pos + 1 < numLEDs) {
                    ledColors[pos + 1] = Color.rgb((int)(255 * 0.75), 0, 0); // 75% brightness
                }
                if (pos + 2 < numLEDs) {
                    ledColors[pos + 2] = Color.rgb((int)(255 * 0.50), 0, 0); // 50% brightness
                }
                if (pos + 3 < numLEDs) {
                    ledColors[pos + 3] = Color.rgb((int)(255 * 0.25), 0, 0); // 25% brightness
                }

                // Update the moving index so that on the next frame the bright pixel moves further left.
                updateMovingIndexForRunRed();
            }
            break;
            case 8: // RAW_NOISE: //slower
                // Simulate rainbow pixels moving back and forth with random speed/destinations.
                // Fade existing LEDs.
                for (int i = 0; i < numLEDs; i++) {
                    ledColors[i] = fadeColor(ledColors[i], 0.9f);
                }
                // Randomly light 1-4 LEDs with random rainbow colors.
                int count = random.nextInt(4) + 1;
                for (int i = 0; i < count; i++) {
                    int randomPos = random.nextInt(numLEDs);
                    float hue = random.nextFloat() * 360f;
                    ledColors[randomPos] = Color.HSVToColor(new float[]{hue, 1f, 1f});
                }
                break;
            case 9: // MOVING_RAINBOW: // slower
                float shift2 = (float)(Math.sin(System.currentTimeMillis() * 0.02) * 180 + 180);
                for (int i = 0; i < numLEDs; i++) {
                    float hue = (((float)i / numLEDs) * 360f + shift2) % 360f;
                    ledColors[i] = Color.HSVToColor(new float[]{hue, 1f, 1f});
                }
                updateMovingIndexWithRandomDelta(5); // Increase speed: use a higher delta
                break;
            case 10: // WAVE: //full fix
            {
                // 1. Set the rainbow background for all LEDs:
                for (int i = 0; i < numLEDs; i++) {
                    float hue = (i / (float) numLEDs) * 360f;
                    ledColors[i] = Color.HSVToColor(new float[]{hue, 1f, 1f});
                }

                // 2. Overlay the moving highlight:
                // We'll brighten the LED at the current moving index.
                // Use the background color for that index and brighten it.
                int baseColor = ledColors[movingIndex];
                ledColors[movingIndex] = lightenColor(baseColor, 0.5f); // Increase brightness by 50%

                // 3. Update the moving index:
                if (movingForward) {
                    movingIndex++;
                    if (movingIndex >= numLEDs - 1) {
                        movingForward = false;
                    }
                } else {
                    movingIndex--;
                    // When the moving index reaches the midpoint, reset to 0:
                    if (movingIndex <= numLEDs / 2) {
                        movingIndex = 0;
                        movingForward = true;
                    }
                }
            }
            break;
            case 11: // BLIGHTB: slightly faster;
                // Similar to BLUEB, but use a lighter blue.
                for (int i = 0; i < numLEDs; i++) {
                    ledColors[i] = fadeColor(ledColors[i], 0.95f);
                }
                ledColors[movingIndex] = lightenColor(Color.parseColor("#3498DB"), 0.2f);
                updateMovingIndex();
                break;
            default:
                Arrays.fill(ledColors, Color.BLACK);
                break;
        }
    }
    private void updateTwinkle() {
        for (int i = 0; i < numLEDs; i++) {
            if (!isTwinkling[i] && random.nextFloat() < 0.13f)
                isTwinkling[i] = true;

            if (isTwinkling[i]) {
                ledBrightnessLevels[i] += 0.05f; // increase brightness fast
                if (ledBrightnessLevels[i] >= 1f) {
                    ledBrightnessLevels[i] = 1f;
                    isTwinkling[i] = false; // start fading
                }
            } else {
                ledBrightnessLevels[i] *= 0.9f; // fade out slowly
            }

            // Assign color based on brightness
            if (ledBrightnessLevels[i] > 0.01f) {
                int paletteIndex = i % twinklePalette.length; // stable palette choice per LED
                int baseColor = twinklePalette[paletteIndex];
                ledColors[i] = adjustBrightness(baseColor, ledBrightnessLevels[i]);
            } else {
                ledColors[i] = Color.BLACK;
            }
        }
    }
    private int adjustBrightness(int color, float brightnessFactor) {
        int a = Color.alpha(color);
        int r = (int)(Color.red(color) * brightnessFactor);
        int g = (int)(Color.green(color) * brightnessFactor);
        int b = (int)(Color.blue(color) * brightnessFactor);
        return Color.argb(a, r, g, b);
    }
    private void updateNebulaSurge() {
        breathingProgress += breathingSpeed;

        if (breathingProgress > 2 * Math.PI) {
            breathingProgress -= 2 * Math.PI;
        }

        float pulse = (float)(Math.sin(breathingProgress) * 0.5f + 0.5f);
        float mappedPulse = 0.6f + 0.69f * pulse; // Your mapped breathing range

        int baseColor = Color.parseColor("#8E05C2"); // Purple

        for (int i = 0; i < numLEDs; i++) {
            // Very tiny flicker per LED
            float flickerChance = random.nextFloat();
            float flicker = 1f;
            if (flickerChance < 0.15f) { // 15% chance per frame
                flicker = 0.97f + random.nextFloat() * 0.03f; // Flicker between 97% - 100%
            }

            // Apply breathing brightness and flicker together
            float finalBrightness = mappedPulse * flicker;

            ledColors[i] = adjustBrightness(baseColor, finalBrightness);
        } //Dark Surge a little brighter
    }
    
    private void updateTransitionWithBreak() {
        int backgroundColor = Color.rgb(93, 58, 147); // #5D3A93
        int brightPurple = Color.rgb(127, 1, 247);    // #7F01F7
        int brightBlue = Color.rgb(41, 134, 204);     // #2986CC

        // Fill background
        Arrays.fill(ledColors, backgroundColor);

        // Draw current dot
        if (movingForward) {
            ledColors[movingIndex] = brightBlue;
        } else {
            ledColors[movingIndex] = brightPurple;
        }

        // Slow down movement
        transitionFrameCounter++;
        if (transitionFrameCounter >= framesPerMove) {
            transitionFrameCounter = 0; // reset counter

            if (movingForward) {
                if (movingIndex < numLEDs - 1) {
                    movingIndex++;
                } else {
                    movingForward = false;
                    movingIndex--;
                }
            } else {
                if (movingIndex > 0) {
                    movingIndex--;
                } else {
                    movingForward = true;
                    movingIndex++;
                }
            }
        }
    }

    private int frameCounter = 0;
    private void updateBlueB() {
        // Fade LEDs gently
        for (int i = 0; i < numLEDs; i++) {
            ledColors[i] = gentleFadeColorFixed(ledColors[i], 10);
        }

        // Faster movement
        frameCounter += 3;

        // Faster full cycle
        double phase = (frameCounter % 40) / 20.0 * Math.PI; // one full oscillation every ~40 frames
        float sine = (float) Math.sin(phase);

        int pos = (int) ((sine + 1f) * 0.5f * (numLEDs - 1)); // map sine to LED index

        // Bright moving dot
        ledColors[pos] = Color.rgb(0, 0, 255);
    }


    private int gentleFadeColorFixed(int color, int fadeAmount) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Only fade if the pixel has brightness
        if (r != 0 || g != 0 || b != 0) {
            r = (r * (255 - fadeAmount)) / 255;
            g = (g * (255 - fadeAmount)) / 255;
            b = (b * (255 - fadeAmount)) / 255;
        }

        return Color.argb(a, r, g, b);
    }







    private int gentleFadeColor(int color, float fadeFactor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        if (r > 0 || g > 0 || b > 0) {
            r = Math.max((int)(r * fadeFactor), 0);
            g = Math.max((int)(g * fadeFactor), 0);
            b = Math.max((int)(b * fadeFactor), 0);
        }

        return Color.argb(a, r, g, b);
    }



    private void updateMovingIndex() {
        if (movingForward) {
            movingIndex++;
            if (movingIndex >= numLEDs) {
                movingIndex = numLEDs - 1;
                movingForward = false;
            }
        } else {
            movingIndex--;
            if (movingIndex < 0) {
                movingIndex = 0;
                movingForward = true;
            }
        }
    }












    private void updateMovingIndexForRunRed() {
        movingIndex++;
        if (movingIndex >= numLEDs) {
            movingIndex = 0;
        }
    }

    private void updateMovingIndexWithRandomDelta(int maxDelta) {
        int delta = random.nextInt(maxDelta) + 1; // delta between 1 and maxDelta
        if (movingForward) {
            movingIndex += delta;
            if (movingIndex >= numLEDs) {
                movingIndex = numLEDs - 1;
                movingForward = false;
            }
        } else {
            movingIndex -= delta;
            if (movingIndex < 0) {
                movingIndex = 0;
                movingForward = true;
            }
        }
    }

    private void updateMovingIndexForWave(int midPoint) {
        if (movingForward) {
            movingIndex++;
            if (movingIndex > midPoint) {
                movingIndex = midPoint;
                movingForward = false;
            }
        } else {
            movingIndex--;
            if (movingIndex < 0) {
                movingIndex = 0;
                movingForward = true;
            }
        }
    }

    /**
     * Helper method to fade a color by a given factor.
     * @param color the initial color
     * @param factor the factor (e.g., 0.9f reduces brightness by 10%)
     * @return the faded color
     */
    private int fadeColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.max((int)(Color.red(color) * factor), 0);
        int g = Math.max((int)(Color.green(color) * factor), 0);
        int b = Math.max((int)(Color.blue(color) * factor), 0);
        return Color.argb(a, r, g, b);
    }

    /**
     * Helper method to blend two colors by a ratio.
     * @param color1 first color
     * @param color2 second color
     * @param ratio  ratio from 0 to 1 (0 gives color1, 1 gives color2)
     * @return the blended color
     */
    private int blendColors(int color1, int color2, float ratio) {
        int a = (int)(Color.alpha(color1) * (1 - ratio) + Color.alpha(color2) * ratio);
        int r = (int)(Color.red(color1) * (1 - ratio) + Color.red(color2) * ratio);
        int g = (int)(Color.green(color1) * (1 - ratio) + Color.green(color2) * ratio);
        int b = (int)(Color.blue(color1) * (1 - ratio) + Color.blue(color2) * ratio);
        return Color.argb(a, r, g, b);
    }

    private int lightenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min((int)(Color.red(color) * (1 + factor)), 255);
        int g = Math.min((int)(Color.green(color) * (1 + factor)), 255);
        int b = Math.min((int)(Color.blue(color) * (1 + factor)), 255);
        return Color.argb(a, r, g, b);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int ledSpacing = width / numLEDs;
        int ledRadius = (ledSpacing / 2) - 2;
        int centerY = height / 2;
        for (int i = 0; i < numLEDs; i++) {
            ledPaint.setColor(ledColors[i]);
            float cx = i * ledSpacing + ledSpacing / 2f;
            canvas.drawCircle(cx, centerY, ledRadius, ledPaint);
        }
    }
}
