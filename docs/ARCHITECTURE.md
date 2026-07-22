# Architecture -- BLE NUS Bridge

## Directory Structure

```
app/src/main/java/com/example/ble_nus_bridge/
├── MainActivity.java      -- UI and BLE scan
├── BridgeService.java     -- Foreground service, BLE <-> TCP bridge
└── res/layout/
    └── activity_main.xml  -- UI layout
```

## MainActivity

`MainActivity` extends `Activity` and implements `View.OnClickListener`.

### BLE Scan

Uses `BluetoothLeScanner` with `ScanCallback` to discover nearby devices.
Auto-stops after 12 seconds. Found devices appear as dynamic buttons in a
`LinearLayout` (`deviceList`). A `HashMap<Button, BluetoothDevice>`
(`buttonDeviceMap`) maps each button to its device.

After scan completes, the device count is displayed. If none found, shows
"No BLE devices found".

### Paired Devices

On `onCreate`, loads paired devices via
`bluetoothAdapter.getBondedDevices()`. The "Paired" button (`refreshBtn`)
reloads this list.

### Permissions

| Android version | Permissions requested |
|-----------------|----------------------|
| 12+ (API 31+) | `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION` |
| 13+ (API 33+) | `POST_NOTIFICATIONS` |

### Broadcast Receiver for Data Preview

Registers a local (non-exported) `BroadcastReceiver` for the
`"DATA_RECEIVED"` intent. `BridgeService` sends received BLE data to this
receiver, which updates the `dataLog` `TextView` with a hex/string preview
and byte count.

### Connection

`connectToDevice(BluetoothDevice)` starts `BridgeService` as a foreground
service via `startForegroundService`, passing the device address as an
extra.

### UI

Layout defined in `activity_main.xml`:

| Element | ID | Purpose |
|---------|-----|---------|
| Header | `title` | "BT SPP Bridge" title |
| Status | `statusText` | Scan/connection status |
| Device list | `deviceList` | Vertical container for device buttons |
| Scan button | `scanBtn` | Triggers BLE scan |
| Paired button | `refreshBtn` | Reloads paired devices |
| Data log | `dataLog` | Monochrome terminal on dark background for real-time data |

## BridgeService

`BridgeService` extends `Service` and runs in the foreground with a
persistent notification.

### BLE GATT Client

- Connects to a BLE device with the exact name `"track-kinesis"`
- Scans with `ScanSettings` in `SCAN_MODE_LOW_LATENCY` (no UUID filter --
  the ESP32 advertises the UUID in scan response)
- After connecting, requests MTU of 256 bytes so complete IMU frames
  (~110 bytes) fit in a single notification
- Service discovery is triggered in `onMtuChanged`
- Enables notifications on the TX characteristic by writing to the CCCD
  (`00002902-0000-1000-8000-00805F9B34FB`)
- Sets connection priority to `CONNECTION_PRIORITY_HIGH`
- On disconnect, restarts scan automatically

### TCP Server

- Starts a `ServerSocket` on port **8090**
- Accepts one TCP connection at a time
- `TcpAcceptor` thread waits for connections; `TcpReader` reads from the
  socket and writes to the BLE RX characteristic
- On client connect, writes `"connected\n"` over the TCP socket
- On TCP disconnect, waits for a new connection (does not stop the service)

### Bidirectional Bridge

| Direction | Source | Destination | Mechanism |
|-----------|--------|-------------|-----------|
| BLE -> TCP | `onCharacteristicChanged` (TX NUS) | `OutputStream` of socket | `sendToTcp()` |
| TCP -> BLE | `InputStream` of socket | `rxCharacteristic` (RX NUS) | `writeCharacteristic()` |

### Notification

- Channel: `"bridge_service"` with `IMPORTANCE_LOW`
- Persistent notification (`setOngoing(true)`) updated with status:
  "Connecting...", "Searching for track-kinesis...", device name
  + " | TCP :8090"
- `PendingIntent` returns to `MainActivity`

### Lifecycle

- `onStartCommand`: receives device address, starts foreground, spawns
  `BleConnector` thread
- `onDestroy`: closes GATT, `ServerSocket`, TCP socket, stops foreground,
  stops service

## NUS UUIDs (Nordic UART Service)

| Description | UUID |
|-------------|------|
| NUS Service | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` |
| TX (ESP32 -> phone, notify) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` |
| RX (phone -> ESP32, write) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` |
| CCCD | `00002902-0000-1000-8000-00805F9B34FB` |

## Build System

| Setting | Value |
|---------|-------|
| Build tool | Gradle + Android Gradle Plugin 8.13.2 |
| Version catalog | `gradle/libs.versions.toml` |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |
| Java compatibility | Java 11 in `compileOptions` |
| Dependencies | `appcompat:1.6.1` |
