# bt-spp-bridge

Android APK source for the BLE NUS Bridge.

See [Installation](../docs/INSTALL.md) for build instructions.
See [Architecture](../docs/ARCHITECTURE.md) for internal design.

## Files

| Path | Purpose |
|------|---------|
| `app/build.sh` | Build script (bash) |
| `app/src/main/AndroidManifest.xml` | App manifest |
| `app/src/main/java/com/termux/bridge/MainActivity.java` | Device scan UI |
| `app/src/main/java/com/termux/bridge/BridgeService.java` | BLE GATT client + TCP bridge |
| `app/src/main/res/values/strings.xml` | String resources |
