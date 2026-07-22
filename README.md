# BLE-NUS-Bridge

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Android app that bridges BLE NUS (Nordic UART Service) to TCP on localhost:8090.
Connect from any TCP client — Termux, ADB shell, or local apps — to read/write data
from BLE devices advertising the NUS service.

## Description

BLE-NUS-Bridge is an Android application that scans for BLE peripherals
advertising the Nordic UART Service (NUS) — UUID `6E400001-B5A3-F393-E0A9-E50E24DCCA9E`.
Once connected, it starts a TCP server on **port 8090** and bridges data
bidirectionally:

- Data received from the BLE device's TX characteristic is forwarded to all
  connected TCP clients.
- Data received from TCP clients is written to the BLE device's RX characteristic.

The app is primarily designed to work with ESP32 devices running NUS firmware
that advertise as **"track-kinesis"**.

## Features

- Scans for BLE NUS peripherals and displays discovered devices
- Connects to a selected device and writes `"connected\n"` to the RX characteristic
- Starts a TCP server on `0.0.0.0:8090` upon connection
- Bidirectional data bridging: BLE TX ↔ TCP clients
- Multiple concurrent TCP clients supported
- Foreground service with notification for reliable background operation
- Automatic cleanup: stops TCP server and disconnects BLE on disconnect

## Requirements

- Android 8.0+ (API level 26)
- Device with BLE support
- ESP32 (or other peripheral) running NUS firmware, advertising as
  `"track-kinesis"` (or any name containing the search filter)

## Building

1. Open the project in **Android Studio**.
2. Wait for Gradle sync to complete (AGP 8.13.2, Gradle 8.14).
3. Select your target device and click **Run** (or use `./gradlew assembleDebug`).

## Usage

1. Launch the app on your Android device.
2. Tap a discovered BLE device from the list.
3. Once connected, the TCP server starts on **port 8090**.
4. Connect from any TCP client:

```bash
nc localhost 8090
```

Type a message and press Enter — it will be written to the BLE device's RX
characteristic. Data arriving from the BLE TX characteristic will be printed
to your TCP client.

## Architecture

```text
BLE Device (NUS Server) <--BLE GATT--> Android App <--TCP:8090--> Termux / ADB / Apps
```

- **MainActivity**: Scans for BLE devices, manages the device list UI.
- **BleBridgeService** (foreground service): Holds the BLE GATT connection
  and the TCP server. Bridges bytes between BLE and TCP.
- **TcpServer**: Accepts TCP clients on port 8090. Reads from clients and
  writes to BLE RX; receives BLE TX data and broadcasts to all clients.
- **DeviceScanAdapter**: RecyclerView adapter for discovered BLE devices.

### Service lifecycle

| Action | Effect |
|--------|--------|
| Device tapped | Connects GATT, writes `"connected\n"`, starts TCP server |
| Disconnect | Stops TCP server, tears down GATT, returns to scan |
| App swiped away | Service stops (no persistent background) |

### Data flow

```
BLE TX characteristic --> onCharacteristicChanged --> TcpServer.broadcast()
                                                          |
                                                     TCP clients

TCP client send --> TcpServer --> writeCharacteristic(RX) --> BLE device
```

## License

MIT
