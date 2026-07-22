# Troubleshooting -- BLE NUS Bridge

| Symptom | Cause | Fix |
|---------|-------|-----|
| BLE scan finds no devices | Location permission not granted | Go to **Settings > Apps > BLE NUS Bridge > Permissions** and enable Location |
| BLE scan finds no devices | Bluetooth is off | Enable Bluetooth in device settings |
| BLE scan finds no devices | Target device is not advertising | Verify the device is powered on and advertising via BLE |
| Connection fails when selecting device | Device name is not exactly `"track-kinesis"` | Confirm BLE device name matches `track-kinesis` (case-sensitive) |
| Connection fails when selecting device | Device is out of range | Move the device closer to the phone |
| Connection fails when selecting device | Device does not advertise Nordic UART Service | Verify firmware announces NUS correctly |
| TCP connection refused | BridgeService is not running in foreground | Check that the "BLE NUS Bridge" notification is visible. If not, reconnect through the app |
| TCP connection refused | Wrong port | Connect using `nc localhost 8090` |
| TCP connection refused | Port 8090 is in use by another process | Stop the other process or change the port |
| Notification does not appear (Android 13+) | `POST_NOTIFICATIONS` not granted | Go to **Settings > Apps > BLE NUS Bridge > Notifications** and enable "Show notifications" |
| Build fails | Gradle JDK not set to JBR 21 | Set **File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK** to the JBR 21 installation (usually `/opt/android-studio/jbr`) |
