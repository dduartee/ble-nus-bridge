# 🔵 BLE NUS Bridge — Proof of Concept

> **Comunicação Bluetooth BLE NUS via Android + Termux + Linux, com desenvolvimento assistido por IA.**
>
> Android app nativo (build no Termux, sem PC) atua como ponte BLE NUS ↔ TCP.
> Permite que o Termux leia/escreva dados de dispositivos BLE GATT (ESP32 como NUS Server)
> via `nc localhost 8090`.

---

## 🎯 Conceito

```
ESP32-S3 (NUS Server) ←→ BLE NUS (GATT Notify/Write) ←→ Android S23 (BLE Bridge App) ←→ TCP :8090 ←→ Termux (Python session_recorder.py)
```

**Fluxo:** Dados trafegam bidirecionalmente entre qualquer ponta — o app Android é o hub central que traduz Bluetooth ↔ TCP.

---

## 📦 Estrutura do Projeto

```
spp-t470/                          ← repositório raiz
│
├── README.md                      ← este documento
├── SUCCESS_REPORT.md              ← relatório de debug T470↔S23
│
└── 📱 LADO S23 (Android/Termux) ─────────────────────────
    (App migrado de BT SPP Classic para BLE NUS — Nordic UART Service)
    └── bt-spp-bridge/
        ├── README.md              ← guia de build no Termux
        ├── TERMUX_API_GUIA.md     ← referência Termux:API
        ├── esp32_bt_temp.ino      ← teste ESP32 Bluetooth
        └── app/
            ├── build.sh           ← build do APK (1 comando)
            ├── PLANO.md           ← log de desenvolvimento (18 erros)
            ├── SUCESSO.md         ← documentação completa
            ├── REVIEW.json        ← análise estruturada (JSON)
            ├── GUIA_T470.md       ← guia original do servidor
            └── app/src/main/
                ├── AndroidManifest.xml
                ├── res/values/strings.xml
                └── java/com/termux/bridge/
                    ├── MainActivity.java      ← UI (lista + scan)
                    └── BridgeService.java     ← foreground service
```

---

## 🚀 Uso Rápido

### 1. Build do APK

**Opção A — No Manjaro:**
```bash
cd bt-spp-bridge/app
bash build.sh                # gera build/bt-spp-bridge.apk (20 KB)
# Transfere pro S23:
scp -P 2222 build/bt-spp-bridge.apk u0_a471@10.0.0.35:~/storage/downloads/
```

**Opção B — No Termux (S23):**
```bash
cd ~/projetos/bt-spp-bridge/app
bash build.sh                # build 100% nativo no Termux
```

**Setup Manjaro (já feito):** JDK 21 Temurun (`~/jdk21/`), Android SDK (`~/android-sdk/`), build-tools 30.0.3, platforms 33+36.

### 2. S23 — App + Termux

```bash
# Instalar pelo gerenciador de arquivos
# Abrir o app → escaneia BLE → tocar em "track-kinesis" na lista
# Bridge BLE NUS conecta automaticamente
# No Termux:
nc localhost 8090            # bridge ativa!
```

### 3. Teste bidirecional

```
ESP32:    "track-kinesis" enviando dados IMU via BLE NUS notify
Termux:   recebe dados JSON via TCP :8090 ✅
Termux:   envia >cmd:start → BLE NUS write → ESP32 recebe ✅
```

---

## 🧠 Desenvolvimento Assistido por IA

Todo o projeto foi implementado por **agentes pi** trabalhando em paralelo:

```
┌─────────────────────────────────────────────────────┐
│                 ORQUESTRADOR (pi)                    │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │ Agente T470  │  │ Agente S23   │  │ Reviewer    │ │
│  │ (Linux/BLE)  │  │ (Android)    │  │ (fanout)    │ │
│  │              │  │              │  │             │ │
│  │ session_rec  │  │ APK build    │  │ docs        │ │
│  │ debug I/O    │  │ Termux java  │  │ revisão     │ │
│  └──────┬───────┘  └──────┬───────┘  └─────┬───────┘ │
│         │                 │                 │         │
│         └─────────┬───────┘                 │         │
│                   │                         │         │
│            ┌──────▼──────┐                  │         │
│            │  INTERCOM   │◄─────────────────┘         │
│            │  relatórios │                             │
│            │  debug logs │                             │
│            └─────────────┘                             │
└─────────────────────────────────────────────────────┘
```

### Agentes envolvidos

| Agente | Responsabilidade | Artefatos |
|--------|-----------------|-----------|
| **T470 (Linux)** | Pipeline de análise, recepção TCP, session_recorder.py | `session_recorder.py`, `plot_analysis.py` |
| **S23 (Android)** | APK nativo no Termux, BLE GATT NUS Client, UI | `MainActivity.java`, `BridgeService.java`, `build.sh` |
| **Reviewer (fanout)** | Análise de erros, documentação, integração | `PLANO.md`, `REVIEW.json`, `README.md` |

### Comunicação entre agentes

- **Intercom** — relatórios de debug, descobertas, handoffs
- **SCP via SSH** — transferência de arquivos entre dispositivos
- **Markdown** — documentação viva atualizada a cada iteração

---

## 🔧 Stack Técnica

### Lado T470 (Linux)

| Camada | Tecnologia | Detalhe |
|--------|-----------|---------|
| Recepção TCP | `socket :8090` | Recebe dados JSON do app bridge via localhost |
| Análise | NumPy + Pandas | Pipeline de detecção de repetições |
| Plotagem | Matplotlib | Multi-panel com eixo dominante destacado |
| Runtime | Python 3.14 | Manjaro Linux, kernel 6.12 |

### Lado S23 (Android/Termux)

| Camada | Tecnologia | Detalhe |
|--------|-----------|---------|
| Build | `aapt2` + `javac` + `dx` + `apksigner` | 100% nativo no Termux ARM64 |
| BLE Scan | `BluetoothLeScanner` | Sem ScanFilter — ESP32 anuncia UUID no scan response |
| BLE GATT | `BluetoothGatt` | connectGatt() + discoverServices() + setCharacteristicNotification() |
| NUS TX Char | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` | Subscribe via notify → dados do ESP32 |
| NUS RX Char | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` | Write sem resposta → comandos phone→ESP32 |
| MTU | 256 bytes | `gatt.requestMtu(256)`, discoverServices em onMtuChanged |
| TCP Server | `ServerSocket :8090` | Foreground service |
| UI | `LinearLayout` programático | Lista de dispositivos BLE + preview de dados (BroadcastReceiver) |
| SDK | platform-33 (aapt2) + platform-36 (javac) | SDKs baixados via curl no Termux |

### Ponte de conexão (BLE NUS)

| Serviço / Característica | UUID | Função |
|--------------------------|------|--------|
| NUS Service | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` | Nordic UART Service |
| TX Characteristic (Notify) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` | ESP32 → phone (dados IMU, sensores) |
| RX Characteristic (Write) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` | Phone → ESP32 (comandos `>cmd:start`, `>cmd:stop`) |

---

## 🐛 Bugs Resolvidos

### Migração BLE (Etapa 9 — app Android reescrito)

| # | Bug | Correção |
|---|-----|----------|
| 1 | Scan sem UUID filter permite encontrar ESP32 | ESP32 anuncia UUID no scan response (31-byte budget separado) |
| 2 | discoverServices() precisa de GATT conectado | Movido para callback onMtuChanged após requestMtu(256) |
| 3 | setCharacteristicNotification() requer descriptor escrito | callback onDescriptorWrite() confirma notify ativo |
| 4 | NUS TX notify entrega payloads fragmentados | Buffer de reassemblagem no onCharacteristicChanged() |
| 5 | MainActivity sem visibilidade dos dados recebidos | BroadcastReceiver DATA_RECEIVED + TextView de preview |

### Histórico SPP (arquivado — 13 bugs originais, ver `PLANO.md`)

| Categoria | Exemplos |
|-----------|----------|
| Build | aapt2 + platform-36 incompatível, d8 bugado, minSdkVersion < 26 |
| Permissões | `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS` |
| Android 14 | `android:exported`, `foregroundServiceType`, `SecurityException` |
| Java | Lambdas (`->`) quebram no Android SDK, usar classes anônimas |

---

## 📊 Especificações do APK

| Atributo | Valor |
|----------|-------|
| Package | `com.termux.bridge` |
| Tamanho | ~20 KB |
| minSdkVersion | 26 (Android 8+) |
| targetSdkVersion | 33 |
| Permissões | Bluetooth×5 (SCAN, CONNECT, ADVERTISE, BLUETOOTH, BLUETOOTH_ADMIN), Location×2, Internet, Foreground×2, Notifications |
| Assinatura | v2 + v3 (debug keystore) |
| Build host | Manjaro Linux (JDK 21 + Android SDK) |

---

## 🧭 Para o Próximo Agente

### Estado atual
- ✅ App Android migrado de SPP Classic para BLE NUS (GATT Client)
- ✅ Bridge BLE NUS ↔ TCP :8090 funcionando (scan, connect, notify, write)
- ✅ MTU 256 negociado, dados IMU trafegam via NUS TX notify
- ✅ Controle de sessão via botão boot (GPIO0) → >cmd:start/stop
- ✅ Pipeline de análise com detecção de repetições por acelerômetro
- ✅ APK buildando no Manjaro (`bt-spp-bridge/app/build.sh`)
- ✅ Repositório git no GitHub: [`dduartee/spp-bt-spp-bridge`](https://github.com/dduartee/spp-bt-spp-bridge)

### Dependências instaladas no T470
| Recurso | Path |
|---------|------|
| JDK 21 (Temurin) | `~/jdk21/` |
| Android SDK | `~/android-sdk/` (platform-33, platform-36, build-tools 30.0.3, platform-tools) |
| Python 3.14 | sistema (Manjaro) |

### Pontos de entrada
| Comando | Função |
|---------|--------|
| `bash bt-spp-bridge/app/build.sh` | Build do APK Android |
| `python3 session_recorder.py` | Receptor TCP + pipeline de análise |

### Pendências (opcionais)
- [x] Migração Android de BT SPP Classic → BLE NUS (GATT Client) — completo
- [x] MTU 256 negociado via requestMtu() — completo
- [x] Controle de sessão por botão boot ESP32 (GPIO0) — completo
- [ ] Adicionar suporte a múltiplos dispositivos BLE simultâneos
- [ ] Cache de dispositivo para reconexão automática
- [ ] Validar input no bridge (limite de tamanho, rate limit)

---

## 🔗 Referências

- [`bt-spp-bridge/README.md`](bt-spp-bridge/README.md) — Guia completo de build no Termux
- [`bt-spp-bridge/app/PLANO.md`](bt-spp-bridge/app/PLANO.md) — Log de desenvolvimento (18 erros)
- [`bt-spp-bridge/app/SUCESSO.md`](bt-spp-bridge/app/SUCESSO.md) — Documentação completa
- [`bt-spp-bridge/app/REVIEW.json`](bt-spp-bridge/app/REVIEW.json) — Análise estruturada
- [`SUCCESS_REPORT.md`](SUCCESS_REPORT.md) — Relatório de debug T470↔S23
- [`bt-spp-bridge/TERMUX_API_GUIA.md`](bt-spp-bridge/TERMUX_API_GUIA.md) — Referência Termux:API
