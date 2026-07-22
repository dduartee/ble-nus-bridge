# Changelog

## 1.0.0 (2026-07-21)

- Migrate to Android Studio project with Gradle build system
- Replace programmatic UI with XML layout (activity_main.xml)
- Replace classic Bluetooth discovery with BLE scanning (BluetoothLeScanner)
- Add proper Gradle version catalog (libs.versions.toml)
- Remove old bt-spp-bridge SPP code and manual build.sh
- Add documentation: ARCHITECTURE.md, INSTALL.md, TROUBLESHOOTING.md, DOCUMENTATION_GUIDE.md
- Add security hardening (setPackage on broadcasts, RECEIVER_NOT_EXPORTED flag)
- Add POST_NOTIFICATIONS permission handling for Android 13+
- Fix BroadcastReceiver lifecycle (proper unregister in onDestroy)
- Write "connected" message to RX characteristic on BLE connection
