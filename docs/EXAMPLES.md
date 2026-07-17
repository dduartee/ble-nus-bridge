# Examples

Usage patterns for the BLE NUS Bridge.

## Basic Bridge

Start the bridge and connect via TCP. The app connects to a BLE device
matching the name configured in `BridgeService.java`.

```bash
# 1. Open app on phone
# 2. Tap the target BLE device in the device list
# 3. App shows "Connected" with TCP port info

# 4. In Termux, connect to the bridge
nc localhost 8090
```

Data from the BLE device appears on stdin as raw bytes.
Text typed to stdin is sent to the BLE device via the RX characteristic.

## Send Commands to BLE Device

```bash
echo "start" | nc localhost 8090
echo "stop"  | nc localhost 8090
```

The BLE device firmware interprets these commands to start/stop
data streaming.

## Read BLE Data

```bash
# Listen continuously
nc localhost 8090

# Pipe to a file
nc localhost 8090 > ble_data.jsonl

# Pipe through jq for formatted output (if JSON)
nc localhost 8090 | jq .
```

## Test Bidirectional Communication

Terminal 1 (Termux):

```bash
nc localhost 8090
```

Terminal 2 (T470 Linux, via SSH):

```bash
ssh phone
echo "ping" | nc localhost 8090
```

If the BLE device echoes back, you will see the response in Terminal 1.

## View Data Preview on Phone

While the bridge is running, the app UI shows:
- Total bytes received
- Last packet size
- Text preview of the most recent data (up to 120 bytes)
- Monospace green-on-dark display

## Check Connection Status

The notification bar shows the current state:

| Status | Notification text |
|--------|------------------|
| Scanning | "Procurando <device>..." |
| Connecting | "Conectando <device>..." |
| Connected | "<device> \| TCP :8090" |
| Disconnected | "Desconectado" |

## Build and Deploy

See [Installation](INSTALL.md) for build and deploy instructions.

## Automation with Script

```bash
#!/bin/bash
# monitor.sh - read BLE data and log to file
LOG_FILE="ble_data_$(date +%Y%m%d_%H%M%S).log"
{
  echo "Session started at $(date)"
  timeout 300 nc localhost 8090
  echo "Session ended at $(date)"
} > "$LOG_FILE" 2>&1
```
