# Instalação — BLE NUS Bridge

## Pré-requisitos

- **Android Studio** (versão atual recomendada)
- **Android SDK 34**
- **JDK 21** (ou 17+ compatível)
- Dispositivo Android físico com BLE (Android 8.0 / API 26+)

## Build e Execução pelo Android Studio

1. Abra o Android Studio e selecione **File → Open**
2. Navegue até a pasta do projeto e clique **OK**
3. Aguarde o Gradle sync completar
4. Conecte um dispositivo Android via USB com depuração USB ativada
5. Clique em **Run** (▶) ou pressione `Shift + F10`

## Build Manual (linha de comando)

```bash
./gradlew assembleDebug
```

O APK gerado estará em:

```
app/build/outputs/apk/debug/app-debug.apk
```

Instale no dispositivo:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissões Necessárias

Ao executar o app pela primeira vez, conceda:

- **Bluetooth** (conexão e scan)
- **Localização** (necessária para BLE scanning no Android)
- **Notificações** (Android 13+) — necessária para o foreground service

## Uso

1. Abra o app
2. Toque em **🔍 Escanear dispositivos**
3. Selecione o dispositivo `track-kinesis` na lista
4. Conecte via TCP:

```bash
nc localhost 8090
```
