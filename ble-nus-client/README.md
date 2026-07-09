# BLE NUS Debugger

Client BLE NUS para T470 — conecta diretamente a qualquer device BLE NUS (ESP32, etc) via bleak.

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install bleak aiohttp
```

## Quick Start

```bash
# Scan for devices
python main.py --scan
python main.py --scan --name track-kinesis

# Interactive CLI
python main.py --cli

# CLI with JSON output (for AI agents)
python main.py --cli --json

# HTTP API server
python main.py --api --port 8091
```

## HTTP API

| Método | Rota | Função |
|--------|------|--------|
| GET | `/scan?timeout=5&name=track` | Escaneia dispositivos BLE |
| POST | `/connect` | `{"addr":"AA:BB:CC:DD:EE:FF"}` |
| POST | `/disconnect` | Desconecta |
| POST | `/send` | `{"data":">cmd:start"}` ou `{"hex":"0003..."}` |
| GET | `/status` | Estado da conexão |
| GET | `/history?limit=50` | Mensagens recebidas |
| WS | `/ws` | Stream de notificações em tempo real |

## CLI Commands

```
scan [name] [timeout]     Scan for BLE devices
connect <addr> [name]     Connect to device
disconnect                Disconnect
send <text>               Send raw text
send-json <json>          Send JSON frame
status                    Connection status
history [n]               Last N messages
clear                     Clear history
help                      Show commands
quit                      Exit
```

## Protocol

ESP32 frames: 2-byte big-endian length prefix + JSON payload.

```json
{"type":"imu","app":"track","ax":0.1,"ay":-9.8,"az":0.2}
```

## Project Structure

```
main.py       entry point
nus.py        NUS UUIDs, frame parser/serializer
ble_conn.py   BLE scan/connect/subscribe (bleak)
api.py        HTTP API + WebSocket (aiohttp)
cli.py        interactive CLI
history.py    message ring buffer
```
