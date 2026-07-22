# Installation -- BLE NUS Bridge

## Prerequisites

- Android Studio (current version recommended)
- Android SDK 34
- JDK 21 (or 17+)
- Physical Android device with BLE (Android 8.0 / API 26+)
- Location services enabled on device (required for BLE scanning)

## Build and Run with Android Studio

1. Open Android Studio and select **File > Open**
2. Navigate to the project folder and click **OK**
3. Wait for Gradle sync to complete
4. Connect an Android device via USB with USB debugging enabled
5. Click **Run** or press `Shift + F10`

## Manual Build (command line)

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install on device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Runtime Permissions

On first launch, grant the following permissions:

| Permission | Required for | Notes |
|------------|-------------|-------|
| Bluetooth (connect + scan) | BLE connection | Android 12+ requires explicit `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` |
| Location | BLE scanning | Android requires location access for BLE scans |
| Notifications | Foreground service persistence | Android 13+ requires `POST_NOTIFICATIONS` |
