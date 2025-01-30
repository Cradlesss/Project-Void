Project Void

🔹Project Void is a Bluetooth Low Energy (BLE)-powered ecosystem consisting of:

  1. Vortex_App – An Android smartphone application (API 24+ compatible) that controls and interacts with connected devices via BLE.
  2. Arduino Firmware – A program running on an nRF52-based board (such as Seeed XIAO nRF52840 / nRF52840 Sense) to handle communication and execute commands.
This project enables seamless connectivity between an Android smartphone and BLE-compatible microcontrollers, allowing for custom lighting controls, animations, and real-time interactions.

🔹How Does It Work?
  1. Download and launch the Vortex_App on your Android device.
  2. Scan for available BLE-compatible devices.
  3. Select the device you want to connect to.
  4. Choose between custom colors (Static Color Mode) or animations, pick the one you like, and send the command.
  5. The Arduino processes the commands and updates the LED strip output accordingly.
  6. The Arduino device sends connection status updates back to the app, ensuring real-time feedback.

🔹 Supported Hardware
   - nRF52-based Boards (e.g., Seeed Xiao nRF52840 / Seeed Xiao nRF52840 Sense)
   - Android Devices (Running API 24+)

🔹Installation & Setup:

  1️⃣ Android App (Vortex_App):
  
      1. Clone the repository:
      
          - git clone https://github.com/yourusername/ProjectVoid.git
            cd Vortex_App
            
      2. Open the project in Android Studio.
      
      3. Ensure you have the necessary dependencies installed:
      
          - Bluetooth & BLE Permissions configured in AndroidManifest.xml
          - Material Components for UI design
          
      4. Connect an Android device (or use an emulator with BLE support).
      
      5. Build and install the APK using:
      
          - ./gradlew installDebug

          
  2️⃣ Arduino Firmware:
  
      1. Clone the repository:
      
          - git clone https://github.com/yourusername/ProjectVoid.git
            cd ArduinoFirmware
            
      2. Install dependencies in the Arduino IDE:
      
          - Adafruit Bluefruit nRF52 Library
          - FastLED
          
      3. Connect your nRF52-based microcontroller via USB.
      
      4. Select the correct board and port in Arduino IDE.
      
      5. Compile and upload the firmware.
      
      
✅ All Done! 
 
🔹 Contributing:
  - Pull requests are welcome! Please follow the contribution guidelines in CONTRIBUTING.md.
  - Need Help? Open an issue on GitHub or reach out via discussions. 
