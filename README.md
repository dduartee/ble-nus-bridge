# BLE-NUS-Bridge

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![AGP](https://img.shields.io/badge/AGP-8.13.2-blue)](build.gradle.kts)
[![minSdk](https://img.shields.io/badge/minSdk-26-orange)](#)

Android app that bridges BLE NUS (Nordic UART Service) to TCP on localhost:8090.

## Prerequisites

- Android 8.0+ (API 26) device with BLE support
- ESP32 running NUS firmware advertising as `track-kinesis`

## Quick Start

```bash
git clone https://github.com/user/BLE-NUS-Bridge.git
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Launch app, tap a discovered device, then connect from any TCP client:

```bash
nc localhost 8090
```

Type a message -- data is bridged bidirectionally between TCP and the BLE device's NUS RX/TX characteristics.

## Features

| Feature | Description |
|---------|-------------|
| BLE Scan | Discovers peripherals advertising the NUS service UUID |
| GATT Connection | Connects to selected device, writes `connected\n` to RX |
| TCP Bridge | Serves bidirectional bridge on `0.0.0.0:8090` |
| Multiple Clients | Supports concurrent TCP connections |
| Foreground Service | Reliable background operation with notification |

See [Architecture](docs/ARCHITECTURE.md) for data flow and service lifecycle.

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| TCP port | `8090` | Local port for the TCP bridge server |
| BLE scan filter | `track-kinesis` | Device name substring for scan filtering |
| NUS TX UUID | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` | BLE characteristic for device-to-phone data |
| NUS RX UUID | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` | BLE characteristic for phone-to-device data |

## Documentation

| Doc | Description |
|-----|-------------|
| [Install](docs/INSTALL.md) | Setup for Android Studio, Gradle, device |
| [Architecture](docs/ARCHITECTURE.md) | Internal design, data flow, components |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common problems and fixes |

## License

MIT
