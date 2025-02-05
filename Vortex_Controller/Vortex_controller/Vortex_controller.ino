#include "FastLED.h"
#include <string.h>
#include <bluefruit.h>

//Number of LEDs
#define NumLeds 100 // 100 target
//LED type:
#define LedType WS2812B
//Color order of LED strip
#define ColorOrder GRB
//data pin:
#define DataOut D4 //pin number 6 on Seeed Xiao Sense D3 in code
//LEDs array
CRGBArray<NumLeds> leds;
CRGB background[NumLeds];
uint8_t noiseData[NumLeds];
//other
uint8_t octaveVal = 2;
uint16_t xVal = 0;
int scaleVal = 50;
uint16_t timeVal = 0;

//all commands so far
enum BluetoothCommands {
  OFF = 0,
  TWINKLE = 1,
  NEBULA_SURGE = 2,
  TRANSITION_WITH_BREAK = 3,
  BLUEB = 4,
  RAINBOW = 5,
  BEATR = 6,
  RUNRED = 7,
  RAW_NOISE = 8,
  MOVING_RAINBOW = 9,
  WAVE = 10,
  BLIGHTB = 11,
  SET_BRIGHTNESS = 12,
  SET_STATIC_COLOR = 13,
  END_BRIGHTNESS_COMMAND = 14
};

//bt command 
int bluetoothValue = OFF; // Store the current running command (default to OFF)
uint8_t brightness = 60; // Brightness value
bool waitingForBrightness = false;
bool waitingForStaticColor = false;
uint8_t staticColor[3];
uint8_t red,green,blue;
int staticColorIndex = 0;

//bt delay fix
unsigned long lastBrightnessUpdate = 0; //Last time command sent
const unsigned long BRIGHTNESS_UPDATE_INTERVAL = 100;//Minimum delay 100ms

//UUIDs
#define CUSTOM_SERVICE_UUID "895dc926-817a-424d-8736-f582d2dbac8e"          //Service UUID
#define ANIMATION_CHAR_UUID  "7953deb4-b2e1-4829-a692-8ec173cc71fc"//"eeff4dba-54e5-4650-b9e0-699d6a35b3bd" //Characteristic UUID

// BLE Service and Characteristic
BLEService customService(CUSTOM_SERVICE_UUID);
BLECharacteristic animationChar(ANIMATION_CHAR_UUID);

///////////////////////////////////////////////////
//Twinkle:

//Twinkle Speed
// From 0 - 8
#define TWINKLE_SPEED 3

//Twinkle Density
//From 0 - 8
#define TWINKLE_DENSITY 8

//How often change collor palettes
#define SECONDS_PER_PALETTE 30

//Background collor
CRGB gBackgroundColor = CRGB::Black;

// If AUTO_SELECT_BACKGROUND_COLOR is set to 1,
// then for any palette where the first two entries
// are the same, a dimmed version of that color will
// automatically be used as the background color.
#define AUTO_SELECT_BACKGROUND_COLOR 1

// If COOL_LIKE_INCANDESCENT is set to 1, colors will
// fade out slighted 'reddened', similar to how
// incandescent bulbs change color as they get dim down.
#define COOL_LIKE_INCANDESCENT 1

CRGBPalette16 party = PartyColors_p;
CRGBPalette16 gCurrentPalette;
CRGBPalette16 gTargetPalette;


void setup() {
  //FaltLED Initialization
  FastLED.addLeds<LedType, DataOut, ColorOrder>(leds, NumLeds).setCorrection(TypicalLEDStrip);
  chooseNextColorPalette(gTargetPalette);

    Serial.begin(115200);
    while(!Serial){
      delay(10);
    }
  Serial.println("Initializing Bluetooth...");
  Bluefruit.begin();
  Bluefruit.setName("Vortex"); //max 8 characters

  //config BLE nad characteristic
  customService.begin();
  animationChar.setProperties(CHR_PROPS_WRITE | CHR_PROPS_WRITE_WO_RESP);
  animationChar.setPermission(SECMODE_OPEN, SECMODE_OPEN);
  animationChar.setWriteCallback(receivedDataCallback);
  animationChar.begin();

  //advertising Bt
  Bluefruit.Advertising.clearData();
  Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);
  Bluefruit.Advertising.addService(customService);
  Bluefruit.Advertising.addName();
  Bluefruit.Advertising.start(0);
  Serial.println("Bt initialized and advertising!");
}

void receivedDataCallback(uint16_t connHandle, BLECharacteristic* chr, uint8_t* data, uint16_t len) {
  if(len>0){
    
    int recievedValue = data[0];

    if(waitingForBrightness){
      //ignoring value 12 in brightness setting  cause its buging
      if(recievedValue == SET_BRIGHTNESS){
        Serial.println("Received 12 while waiting for brightness, ignoring...");
        return;
      }

      unsigned long currentMillis = millis();

      if(currentMillis - lastBrightnessUpdate >= BRIGHTNESS_UPDATE_INTERVAL){
        brightness = recievedValue;
        Serial.print("Received brightness value: ");
        Serial.println(brightness);
        lastBrightnessUpdate = currentMillis;
        bluetoothValue = SET_BRIGHTNESS;
      }

      waitingForBrightness = false;

    } else if(waitingForStaticColor){
      staticColor[staticColorIndex++] = recievedValue;
      if(staticColorIndex == 3){
        red = staticColor[0];
        green = staticColor[1];
        blue = staticColor[2];
        Serial.printf("Static color set to: R=%d, G=%d, B=%d\n", staticColor[0], staticColor[1], staticColor[2]);
        waitingForStaticColor = false;
        staticColorIndex = 0;
        bluetoothValue = SET_STATIC_COLOR;
      }

    }else if(recievedValue == SET_BRIGHTNESS){
      waitingForBrightness = true;
      Serial.println("Waiting for brightness value...");

    }else if(recievedValue == SET_STATIC_COLOR){
      waitingForStaticColor = true;
      staticColorIndex = 0;
      Serial.println("Waiting for static color values...");

    }else{
      bluetoothValue = recievedValue;
      Serial.print("Recieved command: ");
      Serial.println(bluetoothValue);
    }
  }
}

void loop() {
  switch (bluetoothValue ) {
    case OFF:
      LEDsArrayOff();
      break;
    case TWINKLE:
      Twinkle();
      break;
    case NEBULA_SURGE:
      NebulaSurge();
      break;
    case TRANSITION_WITH_BREAK:
      TransitionWithBreak();
      break;
    case BLUEB:
      beat();
      break;
    case RAINBOW:
      RainbowBeat();
      break;
    case BEATR:
      BlurPhaseBeat();
      break;
    case RUNRED:
      SawTooth();
      break;
    case RAW_NOISE:
      FillRawNoise();
      break;
    case MOVING_RAINBOW:
      MovingPixel();
      break;
    case WAVE:
      Wawe();
      break;
    case BLIGHTB:
      Rbeat();
      break;
    case SET_BRIGHTNESS:
      mBrightnes();
      break;
    case SET_STATIC_COLOR:
      applyStaticColor();
      break;
  }
  delay(50);
}

//TwinkleFox drawing mechanism(val=='1') ACSII='49'
void Twinkle() {
  EVERY_N_SECONDS(SECONDS_PER_PALETTE) {
    //chooseNextColorPalette( gTargetPalette );
  }

  EVERY_N_MILLISECONDS(10) {
    nblendPaletteTowardPalette(gCurrentPalette, gTargetPalette, 12);
  }

  drawTwinkles(leds);

  FastLED.show();
}


//  This function loops over each pixel, calculates the
//  adjusted 'clock' that this pixel should use, and calls
//  "CalculateOneTwinkle" on each pixel.  It then displays
//  either the twinkle color of the background color,
//  whichever is brighter.
void drawTwinkles(CRGBSet& L) {
  // "PRNG16" is the pseudorandom number generator
  // It MUST be reset to the same starting value each time
  // this function is called, so that the sequence of 'random'
  // numbers that it generates is (paradoxically) stable.
  uint16_t PRNG16 = 11337;

  uint32_t clock32 = millis();

  // Set up the background color, "bg".
  // if AUTO_SELECT_BACKGROUND_COLOR == 1, and the first two colors of
  // the current palette are identical, then a deeply faded version of
  // that color is used for the background color
  CRGB bg;
  if ((AUTO_SELECT_BACKGROUND_COLOR == 1) && (gCurrentPalette[0] == gCurrentPalette[1])) {
    bg = gCurrentPalette[0];
    uint8_t bglight = bg.getAverageLight();
    if (bglight > 64) {
      bg.nscale8_video(16);  // very bright, so scale to 1/16th
    } else if (bglight > 16) {
      bg.nscale8_video(64);  // not that bright, so scale to 1/4th
    } else {
      bg.nscale8_video(86);  // dim, scale to 1/3rd.
    }
  } else {
    bg = gBackgroundColor;  // just use the explicitly defined background color
  }

  uint8_t backgroundBrightness = bg.getAverageLight();

  for (CRGB& pixel : L) {
    PRNG16 = (uint16_t)(PRNG16 * 2053) + 1384;  // next 'random' number
    uint16_t myclockoffset16 = PRNG16;          // use that number as clock offset
    PRNG16 = (uint16_t)(PRNG16 * 2053) + 1384;  // next 'random' number
                                                // use that number as clock speed adjustment factor (in 8ths, from 8/8ths to 23/8ths)
    uint8_t myspeedmultiplierQ5_3 = ((((PRNG16 & 0xFF) >> 4) + (PRNG16 & 0x0F)) & 0x0F) + 0x08;
    uint32_t myclock30 = (uint32_t)((clock32 * myspeedmultiplierQ5_3) >> 3) + myclockoffset16;
    uint8_t myunique8 = PRNG16 >> 8;  // get 'salt' value for this pixel

    // We now have the adjusted 'clock' for this pixel, now we call
    // the function that computes what color the pixel should be based
    // on the "brightness = f( time )" idea.
    CRGB c = computeOneTwinkle(myclock30, myunique8);

    uint8_t cbright = c.getAverageLight();
    int16_t deltabright = cbright - backgroundBrightness;
    if (deltabright >= 32 || (!bg)) {
      // If the new pixel is significantly brighter than the background color,
      // use the new color.
      pixel = c;
    } else if (deltabright > 0) {
      // If the new pixel is just slightly brighter than the background color,
      // mix a blend of the new color and the background color
      pixel = blend(bg, c, deltabright * 8);
    } else {
      // if the new pixel is not at all brighter than the background color,
      // just use the background color.
      pixel = bg;
    }
  }
}

//  This function takes a time in pseudo-milliseconds,
//  figures out brightness = f( time ), and also hue = f( time )
//  The 'low digits' of the millisecond time are used as
//  input to the brightness wave function.
//  The 'high digits' are used to select a color, so that the color
//  does not change over the course of the fade-in, fade-out
//  of one cycle of the brightness wave function.
//  The 'high digits' are also used to determine whether this pixel
//  should light at all during this cycle, based on the TWINKLE_DENSITY.
CRGB computeOneTwinkle(uint32_t ms, uint8_t salt) {
  uint16_t ticks = ms >> (8 - TWINKLE_SPEED);
  uint8_t fastcycle8 = ticks;
  uint16_t slowcycle16 = (ticks >> 8) + salt;
  slowcycle16 += sin8(slowcycle16);
  slowcycle16 = (slowcycle16 * 2053) + 1384;
  uint8_t slowcycle8 = (slowcycle16 & 0xFF) + (slowcycle16 >> 8);

  uint8_t bright = 0;
  if (((slowcycle8 & 0x0E) / 2) < TWINKLE_DENSITY) {
    bright = attackDecayWave8(fastcycle8);
  }

  uint8_t hue = slowcycle8 - salt;
  CRGB c;
  if (bright > 0) {
    c = ColorFromPalette(gCurrentPalette, hue, bright, NOBLEND);
    if (COOL_LIKE_INCANDESCENT == 1) {
      coolLikeIncandescent(c, fastcycle8);
    }
  } else {
    c = CRGB::Black;
  }
  return c;
}


// This function is like 'triwave8', which produces a
// symmetrical up-and-down triangle sawtooth waveform, except that this
// function produces a triangle wave with a faster attack and a slower decay:
uint8_t attackDecayWave8(uint8_t i) {
  if (i < 86) {
    return i * 3;
  } else {
    i -= 86;
    return 255 - (i + (i / 2));
  }
}

// This function takes a pixel, and if its in the 'fading down'
// part of the cycle, it adjusts the color a little bit like the
// way that incandescent bulbs fade toward 'red' as they dim.
void coolLikeIncandescent(CRGB& c, uint8_t phase) {
  if (phase < 128) return;

  uint8_t cooling = (phase - 128) >> 4;
  c.g = qsub8(c.g, cooling);
  c.b = qsub8(c.b, cooling * 2);
}

// A mostly red palette with green accents and white trim.
// "CRGB::Gray" is used as white to keep the brightness more uniform.
const TProgmemRGBPalette16 RedGreenWhite_p FL_PROGMEM = { CRGB::Red, CRGB::Red, CRGB::Red, CRGB::Red,
                                                          CRGB::Red, CRGB::Red, CRGB::Red, CRGB::Red,
                                                          CRGB::Red, CRGB::Red, CRGB::Gray, CRGB::Gray,
                                                          CRGB::Green, CRGB::Green, CRGB::Green, CRGB::Green };

// A mostly (dark) green palette with red berries.
#define Holly_Green 0x00580c
#define Holly_Red 0xB00402
const TProgmemRGBPalette16 Holly_p FL_PROGMEM = { Holly_Green, Holly_Green, Holly_Green, Holly_Green,
                                                  Holly_Green, Holly_Green, Holly_Green, Holly_Green,
                                                  Holly_Green, Holly_Green, Holly_Green, Holly_Green,
                                                  Holly_Green, Holly_Green, Holly_Green, Holly_Red };

// A red and white striped palette
// "CRGB::Gray" is used as white to keep the brightness more uniform.
const TProgmemRGBPalette16 RedWhite_p FL_PROGMEM = { CRGB::Red, CRGB::Red, CRGB::Red, CRGB::Red,
                                                     CRGB::Gray, CRGB::Gray, CRGB::Gray, CRGB::Gray,
                                                     CRGB::Red, CRGB::Red, CRGB::Red, CRGB::Red,
                                                     CRGB::Gray, CRGB::Gray, CRGB::Gray, CRGB::Gray };

// A mostly blue palette with white accents.
// "CRGB::Gray" is used as white to keep the brightness more uniform.
const TProgmemRGBPalette16 BlueWhite_p FL_PROGMEM = { CRGB::Blue, CRGB::Blue, CRGB::Blue, CRGB::Blue,
                                                      CRGB::Blue, CRGB::Blue, CRGB::Blue, CRGB::Blue,
                                                      CRGB::Blue, CRGB::Blue, CRGB::Blue, CRGB::Blue,
                                                      CRGB::Blue, CRGB::Gray, CRGB::Gray, CRGB::Gray };

// A pure "fairy light" palette with some brightness variations
#define HALFFAIRY ((CRGB::FairyLight & 0xFEFEFE) / 2)
#define QUARTERFAIRY ((CRGB::FairyLight & 0xFCFCFC) / 4)
const TProgmemRGBPalette16 FairyLight_p FL_PROGMEM = { CRGB::FairyLight, CRGB::FairyLight, CRGB::FairyLight, CRGB::FairyLight,
                                                       HALFFAIRY, HALFFAIRY, CRGB::FairyLight, CRGB::FairyLight,
                                                       QUARTERFAIRY, QUARTERFAIRY, CRGB::FairyLight, CRGB::FairyLight,
                                                       CRGB::FairyLight, CRGB::FairyLight, CRGB::FairyLight, CRGB::FairyLight };

// A palette of soft snowflakes with the occasional bright one
const TProgmemRGBPalette16 Snow_p FL_PROGMEM = { 0x304048, 0x304048, 0x304048, 0x304048,
                                                 0x304048, 0x304048, 0x304048, 0x304048,
                                                 0x304048, 0x304048, 0x304048, 0x304048,
                                                 0x304048, 0x304048, 0x304048, 0xE0F0FF };

// A palette reminiscent of large 'old-school' C9-size tree lights
// in the five classic colors: red, orange, green, blue, and white.
#define C9_Red 0xB80400
#define C9_Orange 0x902C02
#define C9_Green 0x046002
#define C9_Blue 0x070758
#define C9_White 0x606820
const TProgmemRGBPalette16 RetroC9_p FL_PROGMEM = { C9_Red, C9_Orange, C9_Red, C9_Orange,
                                                    C9_Orange, C9_Red, C9_Orange, C9_Red,
                                                    C9_Green, C9_Green, C9_Green, C9_Green,
                                                    C9_Blue, C9_Blue, C9_Blue,
                                                    C9_White };

// A cold, icy pale blue palette
#define Ice_Blue1 0x0C1040
#define Ice_Blue2 0x182080
#define Ice_Blue3 0x5080C0
const TProgmemRGBPalette16 Ice_p FL_PROGMEM = {
  Ice_Blue1, Ice_Blue1, Ice_Blue1, Ice_Blue1,
  Ice_Blue1, Ice_Blue1, Ice_Blue1, Ice_Blue1,
  Ice_Blue1, Ice_Blue1, Ice_Blue1, Ice_Blue1,
  Ice_Blue2, Ice_Blue2, Ice_Blue2, Ice_Blue3
};


// Add or remove palette names from this list to control which color
// palettes are used, and in what order.
const TProgmemRGBPalette16* ActivePaletteList[] = {
  //  &RetroC9_p,
  //  &BlueWhite_p,
  &RainbowColors_p,
  //  &FairyLight_p,
  //  &RedGreenWhite_p,
  //  &PartyColors_p,
  //  &RedWhite_p,
  //  &Snow_p,
  //  &Holly_p,
  //  &Ice_p
};


// Advance to the next color palette in the list (above).
void chooseNextColorPalette(CRGBPalette16& pal) {
  const uint8_t numberOfPalettes = sizeof(ActivePaletteList) / sizeof(ActivePaletteList[0]);
  static uint8_t whichPalette = -1;
  whichPalette = addmod8(whichPalette, 1, numberOfPalettes);

  pal = *(ActivePaletteList[whichPalette]);
}

//Transition With Break (val=='3') ASCII='51'
void TransitionWithBreak() {
    static int i = 0;
    static bool forward = true;
    static unsigned long lastUpdate = 0;
    const unsigned long interval = 50; // Adjust speed here
    CRGB backgroundColor = CRGB(93, 58, 147);

    if (millis() - lastUpdate >= interval) {
        lastUpdate = millis();

        // Clear all LEDs
        fill_solid(leds, NumLeds, backgroundColor);

        // Ensure the "break" effect (previous LED off)
        if (forward && i > 0) leds[i - 1] = CRGB(0);
        if (!forward && i < NumLeds - 1) leds[i + 1] = CRGB(0);

        // Set LED color based on direction
        leds[i] = forward ? CRGB(127, 1, 247) : CRGB(41, 134, 204);
        if (!forward && i > 1) leds[i - 1] = CRGB(127, 1, 247);  // Add blue break

        FastLED.show();

        // Move LED position
        if (forward) {
            i++;
            if (i >= NumLeds) {
                forward = false;
                i = NumLeds - 1;
            }
        } else {
            i--;
            if (i < 0) {
                forward = true;
                i = 0;
            }
        }
    }
}

//BeatBlue (val=='4') ASCII='52'
void beat() {
  uint16_t sinBeat = beatsin16(30, 0, NumLeds - 1, 0, 0);
  leds[sinBeat] = CRGB::Blue;
  fadeToBlackBy(leds, NumLeds, 10);
  FastLED.show();
}

//Rainbow (val=='5') ASCII='53'
void RainbowBeat() {
  uint16_t beatA = beatsin16(30, 0, 255);
  uint16_t beatB = beatsin16(20, 0, 255);
  fill_rainbow(leds, NumLeds, (beatA + beatB) / 2, 8);
  FastLED.show();
}

//RainbowBeat (val='6') ASCII='54'
void BlurPhaseBeat() {
  uint8_t sinBeat = beatsin8(30, 0, NumLeds - 1, 0, 0);
  uint8_t sinBeat2 = beatsin8(30, 0, NumLeds - 1, 0, 85);
  uint8_t sinBeat3 = beatsin8(30, 0, NumLeds - 1, 0, 170);
  leds[sinBeat] = CRGB::Green;
  leds[sinBeat2] = CRGB::Blue;
  leds[sinBeat3] = CRGB::Red;
  EVERY_N_MILLISECONDS(1) {
    for (int i = 0; i < 4; i++) {
      blur1d(leds, NumLeds, 50);
    }
  }
  FastLED.show();
}

//RunRed (val=='7') ASCII='55'
void SawTooth() {
  uint8_t hue = 0;
  uint8_t pos = map(beat16(40, 0), 0, 65535, 0, NumLeds - 1);
  leds[pos] = CHSV(hue, 200, 255);
  fadeToBlackBy(leds, NumLeds, 3);
  EVERY_N_MILLISECONDS(10) {
    hue++;
  }
  FastLED.show();
}

//FillRawNoise (val=='8') ASCII='56'
void FillRawNoise() {
  timeVal = millis() / 4;
  memset(noiseData, 0, NumLeds);
  fill_raw_noise8(noiseData, NumLeds, octaveVal, xVal, scaleVal, timeVal);
  for (int i = 0; i < NumLeds; i++) {
    leds[i] = ColorFromPalette(party, noiseData[i], noiseData[NumLeds - i - 1]);
  }
  FastLED.show();
}

//MovingRainbow (val=='9') ASCII='57'
void MovingPixel() {
  drawBackground();
  drawMovingPixel();
  EVERY_N_MILLISECONDS(20) {
    fadeToBlackBy(leds, NumLeds, 10);
    nblend(leds, background, NumLeds, 30);
  }
  FastLED.show();
}

void drawBackground() {
  // A simple plasma effect
  fill_noise16(background, NumLeds, 1, millis(), 30, 1, 0, 50, millis() / 3, 10);
}

void drawMovingPixel() {
  // A pixel that moves back and forth using noise
  uint16_t pos = inoise16(millis() * 100);
  pos = constrain(pos, 13000, 51000);
  pos = map(pos, 13000, 51000, 0, NumLeds - 1);
  leds[pos] = CRGB::Red;
}

//Wawe (val=='a') ASCII='97'
void Wawe() {
  // Waves for LED position
  uint8_t posBeat = beatsin8(30, 0, NumLeds - 1, 0, 0);
  uint8_t posBeat2 = beatsin8(60, 0, NumLeds - 1, 0, 0);
  uint8_t posBeat3 = beatsin16(30, 0, NumLeds - 1, 0, 127);
  uint8_t posBeat4 = beatsin16(60, 0, NumLeds - 1, 0, 127);
  // In the video I use beatsin8 for the positions. For longer strips,
  // the resolution isn't high enough for position and can lead to some
  // LEDs not lighting. If this is the case, use the 16 bit versions below
  // uint16_t posBeat  = beatsin16(30, 0, NUM_LEDS - 1, 0, 0);
  // uint16_t posBeat2 = beatsin16(60, 0, NUM_LEDS - 1, 0, 0);
  // uint16_t posBeat3 = beatsin16(30, 0, NUM_LEDS - 1, 0, 32767);
  // uint16_t posBeat4 = beatsin16(60, 0, NUM_LEDS - 1, 0, 32767);

  // Wave for LED color
  uint8_t colBeat = beatsin8(45, 0, 255, 0, 0);
  leds[(posBeat + posBeat2) / 2] = CHSV(colBeat, 255, 255);
  leds[(posBeat3 + posBeat4) / 2] = CHSV(colBeat, 255, 255);
  fadeToBlackBy(leds, NumLeds, 10);
  FastLED.show();
}

//BeatLightBlue (val=='b') ASCII='98'
void Rbeat() {
  uint16_t sinBeat = beatsin16(30, 0, NumLeds - 1, 0, 0);
  leds[sinBeat] = CRGB(61, 133, 198);
  fadeToBlackBy(leds, NumLeds, 10);
  FastLED.show();
}
//general brightness
void mBrightnes() {
  brightness = constrain(brightness, 48, 60);
  int mappedValue = map(brightness, 48, 60, 0, 255);
  FastLED.setBrightness(mappedValue);
  FastLED.show();
}


//OFF(val=='0') ASCII='48'
void LEDsArrayOff() {
  fill_solid(leds, NumLeds, CRGB::Black);
    FastLED.show();
}

void applyStaticColor(){
  fill_solid(leds, NumLeds, CRGB(red,green,blue));
  FastLED.show();
}

//Transition Jinx (val=='2') ASCII='50'
void NebulaSurge() {
    static uint8_t wave = 0;
    uint8_t brightnessPulse = beatsin8(20, 80, 255); // Slow pulsing brightness effect

    for (int i = 0; i < NumLeds; i++) {
        uint8_t colorIndex = sin8(wave + (i * 15)); // Creates a wave pattern across LEDs
        uint8_t colorShift = beatsin8(10, 0, 255);  // Smooth color shifting effect
        uint8_t flicker = random8(0, 30); // Adds small flickering variation

        // Blend between three colors dynamically over time
        CRGB color = blend(
            blend(CRGB(0x8E05C2), CRGB(0x0061B3), sin8(colorIndex + colorShift) / 255.0),
            CRGB(0x16A085),
            cos8(colorIndex - colorShift) / 255.0
        );

        // Apply brightness pulsing and subtle flickering
        leds[i] = color.nscale8(brightnessPulse - flicker);
    }

    wave += 2; // Adjusts the speed of the animation
    FastLED.show();
}
