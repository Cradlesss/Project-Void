Updates to the app:

22/2/2025
Release 22/2/2025:
    Vortex_app:
        - Created TestingNewActivities.class for testing new UI designs and app functionality;
        - Added a divider line to items in SelectDeviceActivity;
        - Added ic_device;

21/02/2025
Small fixes and redesign for RecyclerView:
    Vortex_app:
        - Replaced AggressiveSnapHelper with CustomSnapHelper.
        - Made a small correction for the padding of the first item in RecyclerView.
        - Renamed test_button_background_carousel to button_background_with_outline.
        - Moved selection_overlay higher in Animations.
        - Added dimens.xml resource.

21/02/2025
Major UI redesign for Animations and StaticColor, and minor redesign for all other activities:
    Vortex_app:
        - Now implementing:
              implementation "com.github.skydoves:colorpickerview:2.3.0"
              implementation 'com.google.android.material:material:1.12.0'
              implementation 'com.github.bumptech.glide:glide:4.16.0'
        - Added a RecyclerView to display all animation buttons in a different way.
        - Added AggressiveSnapHelper for better snapping in the RecyclerView.
        - Added AnimationButtonAdapter for the RecyclerView.
        - Added AnimationItem for better RecyclerView item control.
        - Replaced all current button logic in Animations with RecyclerView logic.
        - Created an animation for the RecyclerView.
        - Added a Forget method to BLEService.
        - Changed PreferenceManager’s deviceCharFlag logic to be updatable only in assignIconBasedOnCharacteristic().
        - Changed PreferenceManager’s deviceName logic to be updatable only in showRenameDialog() and when a new device is discovered.
        - App now relies more on PreferenceManager than on ConnectedDeviceManager.
        - Added a backButton and titleText to all activities.
        - Removed disconnectButton from ActivityMain.
        - Added a popupMenu and a burgerMenuIcon to SelectDeviceActivity and moved the refresh device list to be available under the popupMenu.
        - Added click animations to all buttons.
        - loadDiscoveredDevices() now clears deviceList.
        - Replaced predefinedColors in StaticColorActivity with colorPickerView.
        - StaticColorActivity now displays selected color values also in RGB.
        - Added ic_burger_menu, ic_go_back, on_click_button_effect, replaced_rounded_button (to display animation on click), and test_button_background_carousel.
        - Added burger_menu_popup and a style for the popupMenu in Styles.xml.

19/02/2025
Fixed and changed how deviceName works, added some device settings, and applied minor bug fixes:
    Vortex_app:
        - Created DeviceItemSettings activity that displays basic device settings such as rename, connect/disconnect, and forget.
        - Replaced ConnectedDeviceManager with PreferenceManager in some places for better name control.
        - Added basic methods to PreferenceManager such as: setDeviceName, getDeviceName, setDeviceStatus, getDeviceStatus, and set/getDeviceCharFlag.
        - Changed device leds_icon and replaced default_bluetooth_device_icon with ic_device_default.
        - Added device icons: dialog_background, ic_bluetooth, ic_edit, ic_unpair, and rounded_edit_text.
        - Added a dialog window to rename the device.
        - Added styles.xml.

18/02/2025
Fixed device status and added a timeout to BLEService:
    In Vortex_Controller:
        - Changed main device from Seeed Xiao to Seeed Xiao Sense.
    In Vortex_app:
        - Added a 60-second timeout to BLEService if no device is connected.
        - Reduced connection timeout from 10 to 5 seconds.
        - Added deviceStatusMap to ConnectedDeviceManager.java for checking if a specific device is connected.
        - PreferenceManager now returns the status of a specific device.
        - Minor fixes to how the device is saved.
        - Fixed updateTheUI() to correctly apply the pairButton text and functionality.
        - APG updated from 8.8.0 to 8.8.1.

05/02/2025
Fixed animations and enabled buttons:
    In Vortex_Controller:
        - Minor redesign of how the animations (Jinx and Transition With Break) work; now they are switchable while the animation is running, not only at the end.
        - Renamed animation Jinx to Nebula_Surge.
    In Vortex_app:
        - Re-enabled buttons: Jinx (Nebula_Surge) and Transition With Break.

02/02/2025
Fixed permissions for API 33+:
    In Vortex App:
        - Added permissions compatible with API 33+ and a method for the app to check them.
        - Renamed NotificationChannel.

02/02/2025
Fixed includePairedDevices() displaying unwanted devices:
    In Vortex App:
        - includePairedDevices() was not checking if the paired device was compatible with the app.
        - Minor fixes to sendBrightness(), mainly timing adjustments.

Initial commit:
    - Created GitHub repository and added the first version of Project Void.
