# ROADMAP — PoC SPP Bridge Protocol + BLE NUS App

**Objetivo:** Android (BLE NUS) → Termux (TCP) → Python (análise IMU) + ESP32 (display/controle)
**Abordagem:** TDD estrito, spec-driven, validado exclusivamente por testes automatizados
**Stack:** PlatformIO + Arduino + Unity (testes nativos C++), Python/bats (testes emissor), Android Java (BLE GATT)

**Status geral:** PoC funcional. 58 testes firmware + 21 testes emissor passando.
Especificações SPEC-001 e SPEC-003 em ESTÁVEL (v1.0). SPEC-002 em REVISADA (v0.2) — aguardando validação com hardware ESP32 real.
App Android migrado de SPP Classic para BLE NUS (GATT Client).

---

## Etapa 1 — Definir e documentar o protocolo de mensagens (frame SPP)
[OK] → Pesquisa concluída (4/4 agentes). SPEC-001, SPEC-002, SPEC-003 criadas.
       Decisão: length-prefix framing (2 bytes big-endian + payload UTF-8 JSON)
       Status: SPEC-001 → ESTÁVEL (v1.0) após ciclo de correções

## Etapa 2 — Implementar parser do protocolo em C++ com testes unitários (host/native)
[OK] → 24 testes passando (era 17, expandido no fix cycle).
       Parsing + serialization + free + edge cases (JSON inválido, unicode, campos extras, truncamento).
       Dependência cJSON adicionada para parsing JSON robusto com suporte a \uXXXX.
       SPP_FRAME_MAX_SIZE renomeado: SPP_MAX_PAYLOAD_SIZE + SPP_MAX_FRAME_SIZE.
       Arquivos: src/spp_protocol.cpp, test/test_spp_protocol/test_spp_protocol.cpp
       Referência: SPEC-001-PROTOCOL.md + SPEC-002-FIRMWARE.md

## Etapa 3 — Implementar serializer do protocolo (lado emissor) com testes
[OK] → 24 testes abrangem serializer + parser juntos. Round-trip validado.
       spp_serialize_frame() + spp_free_frame() testados com payloads reais.
       Referência: SPEC-001-PROTOCOL.md

## Etapa 4 — Implementar mock de transporte SPP para os testes (sem hardware)
[OK] → 16 testes passando (era 8, expandido com simulação de fragmentação RFCOMM).
       MockTransport: feed/feed_frame/read/write/capture + fragmentação modo coalescing, parcial, byte-a-byte.
       Transport real com #ifdef UNIT_TEST para desvio para mock.
       Arquivos: test/mocks/MockTransport.h, src/spp_transport.cpp
       Referência: SPEC-003-TRANSPORT.md

## Etapa 5 — Implementar lógica de exibição no ESP32 sobre o protocolo (com mock de display)
[OK] → 10 testes passando. MockDisplay com record_command/command_count/command.
       UICommand values corrigidos (Step 1): alinhados com dev-watch real (CMD_NONE=0, CMD_SHOW_NOTIFICATION_POPUP=8, CMD_REQUEST_NOTIFICATIONS_UPDATE=9, CMD_UPDATE_CONNECTIVITY=20, CMD_LOG_ERROR=21).
       Formato de texto corrigido (Step 2): de "[app] title\nmessage" para "app\ntitle\nmessage".
       Display dispatch: notification→popup+update, pong→connectivity, error→log.
       Arquivos: test/mocks/MockDisplay.h/.cpp, src/spp_display.cpp
       Referência: SPEC-002-FIRMWARE.md (REVISADA v0.2 — pendente validação hardware)

## Etapa 6 — Validar o fluxo completo emitindo uma notificação fake e verificando o output
[OK] → 8 testes E2E passando. Fluxo completo validado: Transport → Parse → Display.
       Destaque: test_e2e_notification_fake — simula notificação Termux real.
       Inclui testes de: múltiplos frames, frame de erro, transporte vazio, recuperação de frame parcial.
       Arquivo: test/test_spp_e2e/test_spp_e2e.cpp
       Referência: SPEC-001 + SPEC-002 + SPEC-003

## Etapa 7 — Implementar emissor Python para Android (Termux mock + real)
[OK] → Emissor Python implementado (termux/emitter.py, 174 linhas).
       Mock receptor SPP Python (termux/mock/spp_receiver.py, 148 linhas).
       Testes shell/bats: 21 testes (9 protocolo + 7 emissor + 5 integração).
       Whitelist, deduplicação por (packageName, id), serialização conforme SPEC-001, reconexão Bluetooth.
       Arquivos: termux/emitter.py, termux/mock/spp_receiver.py, termux/notify.sh

## Etapa 8 — Atualizar status dos specs e roadmap
[OK] → Este documento.
       SPEC-001: ESTÁVEL (v1.0)
       SPEC-002: REVISADA (v0.2) — pendente hardware
       SPEC-003: ESTÁVEL (v1.0)

## Etapa 9 — Migrar app Android de BT SPP Classic para BLE NUS (GATT Client)
[OK] → BridgeService.java reescrito: BluetoothLeScanner substitui BluetoothAdapter.getRemoteDevice.
       Scan sem UUID filter (ESP32 anuncia UUID no scan response, budget 31-byte separado).
       Conexao via BluetoothGatt, subscribe na TX characteristic (Notify) para dados ESP32→phone.
       RX characteristic (Write) para comandos phone→ESP32 (>cmd:start, >cmd:stop).
       MTU 256 negociado via gatt.requestMtu(256), discoverServices() movido para onMtuChanged.
       Bridge TCP :8090 mantido — compativel com session_recorder.py existente.
       MainActivity atualizada com preview de dados recebidos (BroadcastReceiver DATA_RECEIVED).

## Etapa 10 — Integrar controle de sessao por botao
[OK] → ESP32 botao boot (GPIO0) toggle → >cmd:start / >cmd:stop via BLE NUS Write.
       Python session_recorder.py recebe comandos, cria pastas timestamped, salva CSV + analises.
       Display do ESP32 mostra "● GRAVANDO" com timer.

## Etapa 11 — Correcao do pipeline de analise (accel-based rep detection)
[OK] → segment_reps() usa acelerometro do eixo dominante (nao angulo do giroscopio).
       Parametros auto-ajustados (height 25% range, distance via zero-crossings).
       Mapeamento de eixos calibrado: X=Vertical, Y=Lateral, Z=Sagital.
       Plot multi-panel reescrito com destaque para eixo dominante.

---

## Ciclo de Correções (Fix Cycle)

O code review identificou e corrigiu os seguintes problemas entre a execução inicial (43 testes) e a versão final (79+ testes):

| # | Problema | Correção | Spec afetada |
|---|----------|----------|--------------|
| 1 | UICommand enum com valores 0/1/2/3/4 incompatíveis com dev-watch real | Alinhado para 0/8/9/20/21 | SPEC-002 |
| 2 | Formato text do popup usava "[app] title\nmessage" mas dev-watch espera "app\ntitle\nmessage" | Removido colchetes do app, formato corrigido | SPEC-002 |
| 3 | SPP_FRAME_MAX_SIZE nome ambíguo (era usado tanto para payload quanto para frame completo) | Renomeado: SPP_MAX_PAYLOAD_SIZE (65535) + SPP_MAX_FRAME_SIZE (65537) | SPEC-001 |
| 4 | Parser JSON manual sem suporte a \uXXXX (unicode escapes) | cJSON integrado como dependência, parser substituído | SPEC-001 |
| 5 | Emissor Python inexistente (spec-only) | termux/emitter.py implementado com 174 linhas | SPEC-003 |
| 6 | MockTransport sem simulação de fragmentação RFCOMM | Modos de coalescing, parcial, byte-a-byte adicionados | SPEC-003 |
| 7 | Testes de transporte insuficientes para cobrir fragmentação | Expandido de 8 para 16 testes | SPEC-003 |
| 8 | Testes de protocolo insuficientes para cobrir edge cases | Expandido de 17 para 24 testes | SPEC-001 |

---

## Resumo de Testes

```
═══════════════════════════════════════════════════════════
  SPP Bridge — Suite de Testes Completa
═══════════════════════════════════════════════════════════

  FIRMWARE (C++ / PlatformIO / Unity)
  ─────────────────────────────────────────────────────────
  test_spp_protocol        24 testes    Parsing, serialization, schema, free
  test_spp_transport       16 testes    MockTransport, fragmentação RFCOMM
  test_spp_display         10 testes    Dispatch, UICommand, formato texto
  test_spp_e2e              8 testes    Fluxo completo Transport→Parse→Display
  ─────────────────────────────────────────────────────────
  Subtotal firmware        58 testes    100% passando

  EMISSOR (Python / Shell / bats-style)
  ─────────────────────────────────────────────────────────
  test_protocol.sh          9 testes    Header-payload, UTF-8, campos JSON
  test_emitter.sh           7 testes    Whitelist, dedup, serialização
  test_integration.sh       5 testes    Emitter→frame→receptor mock
  ─────────────────────────────────────────────────────────
  Subtotal emissor         21 testes    100% passando

  ─────────────────────────────────────────────────────────
  Total                    79 testes    100% passando

  ANDROID APP (BLE GATT NUS Client)
  ─────────────────────────────────────────────────────────
  BridgeService.java        BLE scan + GATT connect + notify/write + TCP bridge
  MainActivity.java         Device list + data preview (BroadcastReceiver)
  ─────────────────────────────────────────────────────────
  App migrado de SPP Classic para BLE NUS — validado com ESP32 real
═══════════════════════════════════════════════════════════
```

---

## Log de Decisões

| Data | Decisão | Motivada por |
|------|---------|--------------|
| 2026-06-10 | Length-prefix framing (2B BE + payload UTF-8) | Pesquisa: BTstack+TI recomendam; stream SPP não garante pacotes |
| 2026-06-10 | PlatformIO native + Unity + ArduinoFake + mocks manuais | Pesquisa: padrão dev-watch; test_build_src=false evita conflitos |
| 2026-06-10 | Display mock registra comandos (sem LVGL real) | dev-watch usa UIQueue/UIMessage; mock valida dispatch |
| 2026-06-10 | Emissor: polling termux-notification-list a cada 2-5s | Termux:API não tem streaming nativo; JSON output dos notifications |
| 2026-06-10 | Payload: JSON `{type,app,title,message,time}` | Compatível com dev-watch CMD_SHOW_NOTIFICATION_POPUP |
| 2026-06-10 | Parser armazena JSON completo no payload (não só "type") | Necessário para spp_display_dispatch extrair app/title/message |
| 2026-06-10 | `json_get_string()` exportada no header | Reuso entre parser (validação) e dispatch (extração de campos) |
| 2026-06-10 | E2E: spp_bridge_process() helper no arquivo de teste | Encapsula pipeline read→parse→dispatch para testes de integração |

### Decisões do Ciclo de Correções

| Data | Decisão | Motivada por |
|------|---------|--------------|
| 2026-06-10 | UICommand values alinhados com dev-watch real (0/8/9/20/21) | Code review: SPEC-002 usava valores fictícios incompatíveis |
| 2026-06-10 | Formato de texto "app\ntitle\nmessage" sem colchetes | Code review: dev-watch CMD_SHOW_NOTIFICATION_POPUP não espera "[app]" |
| 2026-06-10 | SPP_MAX_PAYLOAD_SIZE + SPP_MAX_FRAME_SIZE em vez de SPP_FRAME_MAX_SIZE | Code review: nome único causava ambiguidade entre payload e frame |
| 2026-06-10 | cJSON como parser JSON (em vez de manual) | Code review: parser manual não tratava \uXXXX nem todos edge cases |
| 2026-06-10 | Modos de fragmentação RFCOMM no MockTransport | Garantir que parser tolera qualquer tamanho de fragmento SPP |
| 2026-06-10 | termux/emitter.py em Python (não shell) | Shell não tem bindings Bluetooth nativos; Python + PyBluez/PySerial |
| 2026-06-10 | 24 testes protocolo (antes 17) | Cobertura insuficiente de edge cases (unicode, truncamento, campos extras) |
| 2026-06-10 | 16 testes transporte (antes 8) | Novo modo fragmentação exigiu mais cenários |

---

## Status dos Specs

| Spec | Status | Versão | Próximo Passo |
|------|--------|--------|---------------|
| SPEC-001-PROTOCOL.md | ESTÁVEL (v1.0) | 1.0 | Nenhum — protocolo validado por 58 testes firmware + 21 testes emissor |
| SPEC-002-FIRMWARE.md | REVISADA (v0.2) | 0.2 | Validar com hardware ESP32 real; CMD_UPDATE_CONNECTIVITY e CMD_LOG_ERROR são extensões PoC |
| SPEC-003-TRANSPORT.md | ESTÁVEL (v1.0) | 1.0 | Nenhum — emissor implementado, mock validado, portabilidade contratada |

---

## Artefatos Produzidos

```
spp-t470/
├── ROADMAP.md                          ← este documento (v9 — BLE NUS + análise)
├── specs/
│   ├── SPEC-001-PROTOCOL.md            ← protocolo de frame SPP (ESTÁVEL v1.0)
│   ├── SPEC-002-FIRMWARE.md            ← firmware ESP32 (REVISADA v0.2)
│   └── SPEC-003-TRANSPORT.md           ← transporte (ESTÁVEL v1.0)
├── research/
│   ├── 01-platformio-native-testing.md
│   ├── 02-bluetooth-spp-framing.md
│   ├── 03-dev-watch-display-api.md
│   └── 04-termux-api-notifications.md
├── termux/
│   ├── emitter.py                      ← emissor Python (174 linhas)
│   ├── notify.sh                       ← script auxiliar de notificação
│   ├── mock/
│   │   └── spp_receiver.py             ← mock receptor SPP Python (148 linhas)
│   └── tests/
│       ├── run_all.sh                  ← executor de todos os testes emissor
│       ├── test_emitter.sh             ← 7 testes (whitelist, dedup)
│       ├── test_protocol.sh            ← 9 testes (frames, UTF-8, campos)
│       ├── test_integration.sh         ← 5 testes (emitter→frame→receptor)
│       └── fixtures/
│           ├── notification_whatsapp.json
│           ├── notification_gmail.json
│           ├── notification_telegram.json
│           ├── notification_empty.json
│           └── notification_malformed.json
└── firmware/
    ├── platformio.ini
    ├── src/
    │   ├── spp_protocol.h / .cpp       ← parser + serializer (cJSON)
    │   ├── spp_transport.h / .cpp      ← transporte (com mock condicional)
    │   └── spp_display.h / .cpp        ← display dispatch
    ├── include/
    │   └── README
    └── test/
        ├── test_spp_protocol/          ← 24 testes
        ├── test_spp_transport/         ← 16 testes (fragmentação RFCOMM)
        ├── test_spp_display/           ← 10 testes
        ├── test_spp_e2e/               ← 8 testes (fluxo completo)
        └── mocks/
            ├── MockTransport.h         ← mock de Bluetooth SPP (com fragmentação)
            ├── MockDisplay.h / .cpp    ← mock de display LVGL
            └── .gitkeep
└── bt-spp-bridge/                      ← app Android migrado p/ BLE NUS (Etapa 9)
    ├── README.md                       ← guia de build no Termux
    ├── TERMUX_API_GUIA.md              ← referência Termux:API
    ├── esp32_bt_temp.ino               ← teste ESP32
    └── app/
        ├── build.sh                    ← script de build do APK
        ├── PLANO.md                    ← log de desenvolvimento
        ├── SUCESSO.md                  ← documentação completa
        ├── REVIEW.json                 ← análise estruturada
        └── app/src/main/
            ├── AndroidManifest.xml
            ├── res/values/strings.xml
            └── java/com/termux/bridge/
                ├── MainActivity.java               ← UI (scan BLE + preview dados)
                └── BridgeService.java              ← GATT NUS Client + TCP bridge
```
