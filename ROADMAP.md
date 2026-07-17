# Roadmap

Android app (BLE NUS GATT Client) that bridges BLE to TCP for Termux.

## Completed: Migrate from SPP Classic to BLE NUS (GATT Client)

BridgeService.java rewritten with BluetoothLeScanner replacing BluetoothAdapter.
- BLE scan and GATT connect/subscribe to TX characteristic (notify)
- RX characteristic (Write) for phone-to-device commands
- MTU 256 negotiated, discoverServices on onMtuChanged
- TCP port 8090 bridge maintained
- MainActivity updated with data preview (BroadcastReceiver DATA_RECEIVED)

## Current State

```text
spp-t470/
├── README.md
├── ROADMAP.md
├── LICENSE
│
├── bt-spp-bridge/
│   ├── README.md
│   └── app/
│       ├── build.sh
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── res/values/strings.xml
│           └── java/com/termux/bridge/
│               ├── MainActivity.java
│               └── BridgeService.java
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── INSTALL.md
│   ├── EXAMPLES.md
│   ├── TROUBLESHOOTING.md
│   └── DOCUMENTATION_GUIDE.md
```
