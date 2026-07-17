# Architecture

BLE NUS Bridge is an Android foreground service that translates between
Bluetooth Low Energy (GATT) and TCP.

## Data Flow

```text
BLE Device                   Android                            Termux
(NUS Server)                 (BLE NUS Bridge)                   (TCP Client)
                              
    |                            |                                 |
    |--- BLE Advertisement --->  |                                 |
    |  (by device name)         |                                 |
    |                            |                                 |
    |  <-- BLE Scan -------      |                                 |
    |                            |                                 |
    |  <-- connectGatt() ---     |                                 |
    |  <-- requestMtu(256) --    |                                 |
    |  <-- discoverServices()    |                                 |
    |  <-- setCharacteristic     |                                 |
    |       Notification(true)   |                                 |
    |  <-- writeDescriptor       |                                 |
    |       (CCCD enable)        |                                 |
    |                            |                                 |
    |  === NUS TX (Notify) ===>  |  === TCP :8090 ==============>  |
    |  (data frames)              |  (forwarded to connected TCP)  |
    |                            |                                 |
    |  <=== NUS RX (Write) ====  |  <=== TCP :8090 ==============  |
    |  (commands)                 |  (from nc or other client)      |
    |                            |                                 |
```

## Components

### BridgeService.java

Foreground service containing the BLE GATT client and TCP server.

| Component | Class | Responsibility |
|-----------|-------|----------------|
| BLE Scanner | `BleConnector` | Scans for the target BLE device by name using `BluetoothLeScanner` |
| GATT Client | `gattCallback` | Manages connection, MTU negotiation, service discovery, notifications |
| TCP Server | `TcpAcceptor` | Listens on port 8090 for incoming TCP connections |
| TCP Reader | `TcpReader` | Reads bytes from TCP client and writes to BLE RX characteristic |

### MainActivity.java

UI for device discovery and data preview.

| Feature | Implementation |
|---------|---------------|
| Device list | Classic Bluetooth discovery (`BluetoothAdapter.startDiscovery()`) |
| Data preview | BroadcastReceiver listening for "DATA_RECEIVED" intent |
| Permissions | Runtime requests for BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION |

## GATT Connection Sequence

1. **Scan**: `BluetoothLeScanner.startScan()` with no UUID filter
2. **Match**: Device name matches the configured target in `onScanResult`
3. **Connect**: `device.connectGatt()` with auto-reconnect = false
4. **MTU Negotiation**: `requestMtu(256)` in `onConnectionStateChange`
5. **Service Discovery**: `discoverServices()` in `onMtuChanged`
6. **Characteristic Setup**: Find NUS TX (notify) and RX (write) characteristics
7. **Notification Enable**: Write CCCD descriptor to enable notifications on TX char
8. **TCP Bridge**: Start `TcpAcceptor` on port 8090

## UUIDs

| Service / Characteristic | UUID | Direction |
|--------------------------|------|-----------|
| Nordic UART Service | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` | - |
| TX Characteristic (Notify) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` | Device -> Phone |
| RX Characteristic (Write) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` | Phone -> Device |
| CCCD Descriptor | `00002902-0000-1000-8000-00805F9B34FB` | Notification enable |

## Threading Model

| Thread | Role | Created By |
|--------|------|------------|
| Main (UI) | Activity lifecycle, permission handling | Android |
| BLE-Scan | Scan + GATT connection | `BridgeService.onStartCommand()` |
| GATT callback | BLE events (connection, MTU, data) | `BluetoothGatt` (Binder thread) |
| TCP-Accept | Accept incoming TCP clients | `startTcpBridge()` |
| TCP2BT | Read TCP socket, write to BLE | `TcpAcceptor` (per client) |

## Build Architecture

```text
               +------------------+
               |    build.sh      |
               |  (bash script)   |
               +--------+---------+
                        |
          +-------------+-------------+
          |             |              |
      aapt2 compile  javac -source 8 -target 8  apksigner
          |             |              |
          v             v              v
    compiled.flata  .class files   signed APK
          |             |
          +------+------+
                 |
             aapt2 link
                 |
           base.apk (unsigned)
                 |
            +----+----+
            |         |
        classes.dex  base.apk
            |         |
            +----+----+
                 |
            apksigner
                 |
           bt-spp-bridge.apk
```

Current constraints:
- **aapt2** uses platform-33 (Termux aapt2 v2.19 cannot load newer platforms)
- **javac** uses platform-36 (newer API surface)
- **dx** instead of d8 (d8 crashes on Termux ARM64 with NullPointerException)
- **--release 8** for Java source/target compatibility
- No lambdas in Java source (d8 limitation)

## Related Docs

- [Installation](INSTALL.md) — build APK
- [Troubleshooting](TROUBLESHOOTING.md) — fix build errors for the constraints above
