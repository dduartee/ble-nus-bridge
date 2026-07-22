# Arquitetura Técnica — BLE NUS Bridge

## Estrutura de Pacotes

```
com.example.ble_nus_bridge/
├── MainActivity.java     — UI e scanning BLE
├── BridgeService.java    — Serviço foreground, bridge BLE ⇄ TCP
└── res/layout/
    └── activity_main.xml — Layout da interface
```

## MainActivity

`MainActivity` estende `Activity` e implementa `View.OnClickListener`.

### Scanning BLE

Usa `BluetoothLeScanner` com `ScanCallback` para descobrir dispositivos próximos. O scan tem auto-stop após **12 segundos**. Dispositivos encontrados são exibidos como botões dinâmicos adicionados ao `LinearLayout` com id `deviceList`. O mapa `buttonDeviceMap` associa cada botão ao seu `BluetoothDevice`.

Após o scan, mostra contagem de dispositivos encontrados. Se nenhum for encontrado, exibe "Nenhum dispositivo BLE encontrado".

### Dispositivos Pareados

Ao iniciar (`onCreate`), carrega dispositivos pareados via `bluetoothAdapter.getBondedDevices()`. O botão "🔄 Pareados" (`refreshBtn`) recarrega esta lista.

### Inicialização & Permissões

- Android 12+ (`Build.VERSION_CODES.S`): solicita `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` e `ACCESS_FINE_LOCATION`
- Android 13+ (`Build.VERSION_CODES.TIRAMISU`): solicita `POST_NOTIFICATIONS`

### Broadcast Receiver para Preview de Dados

Registra um `BroadcastReceiver` local (não exportado) para o intent `"DATA_RECEIVED"`. O `BridgeService` envia dados recebidos via BLE para este receiver, que atualiza o `TextView` `dataLog` com preview em hex/string e contagem de bytes.

### Conexão

O método `connectToDevice(BluetoothDevice)` inicia o `BridgeService` como foreground service via `startForegroundService`, passando o endereço do dispositivo como extra.

### UI

Layout definido em `activity_main.xml`:
- `title` — título "🔵 BT SPP Bridge"
- `statusText` — status do scan/conexão
- `deviceList` — container vertical para botões de dispositivos
- `scanBtn` — botão "🔍 Escanear dispositivos"
- `refreshBtn` — botão "🔄 Pareados"
- `dataLog` — terminal monocromático com fundo escuro para exibição de dados em tempo real

## BridgeService

`BridgeService` estende `Service` e roda em foreground com notificação persistente.

### Componentes

#### BLE GATT Client

- Conecta-se ao dispositivo BLE cujo nome seja exatamente `"track-kinesis"`
- Escaneia com `ScanSettings` em modo `SCAN_MODE_LOW_LATENCY` (sem filtro UUID, pois o ESP32 anuncia o UUID no scan response)
- Após conectar, requisita MTU de **256 bytes** para que quadros IMU completos (~110 bytes) caibam em uma única notificação
- Descoberta de serviços é feita no callback `onMtuChanged`
- Habilita notificações na característica TX escrevendo no CCCD (`00002902-0000-1000-8000-00805F9B34FB`)
- Define prioridade de conexão como `CONNECTION_PRIORITY_HIGH`
- Ao desconectar, reinicia o scan automaticamente

#### Servidor TCP

- Inicia um `ServerSocket` na porta **8090**
- Aceita uma conexão TCP por vez
- Thread `TcpAcceptor` aguarda conexões; `TcpReader` lê dados do socket e escreve na característica RX do BLE

#### Ponte Bidirecional

| Direção | Origem | Destino | Mecanismo |
|---------|--------|---------|-----------|
| BLE → TCP | `onCharacteristicChanged` (TX NUS) | `OutputStream` do socket | `sendToTcp()` |
| TCP → BLE | `InputStream` do socket | `rxCharacteristic` (RX NUS) | `writeCharacteristic()` |

#### Notificação

- Canal: `"bridge_service"` com importância `IMPORTANCE_LOW`
- Notificação persistente (`setOngoing(true)`) atualizada com status: "Conectando...", "Procurando track-kinesis...", nome do dispositivo + " | TCP :8090"
- `PendingIntent` leva de volta à `MainActivity`

#### Ciclo de Vida

- `onStartCommand`: recebe endereço do dispositivo, inicia foreground, dispara thread `BleConnector`
- `onDestroy`: fecha GATT, `ServerSocket`, socket TCP, para foreground e para o serviço

## UUIDs do Nordic UART Service (NUS)

| Descrição | UUID |
|-----------|------|
| Serviço NUS | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` |
| TX (ESP32 → telefone, notify) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` |
| RX (telefone → ESP32, write) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` |
| CCCD | `00002902-0000-1000-8000-00805F9B34FB` |

## Sistema de Build

- **Gradle** com **Android Gradle Plugin 8.13.2**
- **Version catalog**: `gradle/libs.versions.toml`
- **compileSdk / targetSdk**: 34
- **minSdk**: 26
- **Java 11** configurado em `compileOptions`
- Dependências: `appcompat:1.6.1`
