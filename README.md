# BLE NUS Bridge

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Android app that bridges BLE NUS (Nordic UART Service) to TCP localhost.
Allows Termux to read/write data from BLE GATT devices via `nc localhost 8090`.

## Quick Start

```bash
cd bt-spp-bridge/app
bash build.sh
adb install build/bt-spp-bridge.apk
# Open app, tap device, then in Termux:
nc localhost 8090
```

## Prerequisites

| Dependency | Version | Source |
|------------|---------|--------|
| JDK | 21+ | [Temurin](https://adoptium.net/) |
| Android SDK | platform-33 + platform-36 | `curl` from Google |
| Build tools | 30.0.3 (aapt2, dx, apksigner) | Android SDK Manager |
| Target device | Android 8+ (API 26), BLE support | Samsung Galaxy S23 tested |

Full setup: [Installation Guide](docs/INSTALL.md)

## Tools

| Tool | Description |
|------|-------------|
| [`build.sh`](bt-spp-bridge/app/build.sh) | Build APK from source |
| `nc localhost 8090` | Connect to the BLE bridge from Termux |

## Configuration

| Env var | Default | Description |
|---------|---------|-------------|
| `JAVA_HOME` | `$HOME/jdk21` | JDK installation path |
| `ANDROID_HOME` | `$HOME/android-sdk` | Android SDK root |

See [Installation](docs/INSTALL.md#environment-variables) for setup details.

## Architecture

```text
BLE Device (NUS Server) <--BLE GATT--> Android App <--TCP:8090--> Termux
```

The Android app scans BLE devices, connects via GATT, and bridges data
bidirectionally over TCP on port 8090. Details: [Architecture](docs/ARCHITECTURE.md)

## Documentation

| Doc | Description |
|-----|-------------|
| [Install](docs/INSTALL.md) | Build APK on Manjaro or Termux |
| [Architecture](docs/ARCHITECTURE.md) | Internal design and data flow |
| [Examples](docs/EXAMPLES.md) | Usage patterns and workflows |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | Common problems and fixes |
| [Roadmap](ROADMAP.md) | Project status and directory structure |
| [Documentation Guide](docs/DOCUMENTATION_GUIDE.md) | Writing standards for this project |
| [Source layout](bt-spp-bridge/README.md) | APK source files and structure |

## APK Specifications

| Attribute | Value |
|-----------|-------|
| Package | `com.termux.bridge` |
| minSdkVersion | 26 (Android 8+) |
| targetSdkVersion | 33 |
| Permissions | BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_CONNECTED_DEVICE, POST_NOTIFICATIONS |
| Build host | Manjaro Linux (JDK 21 + Android SDK) |

## License

MIT
