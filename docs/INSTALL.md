# Installation

Build and install the BLE NUS Bridge APK.

## Prerequisites

### Manjaro Linux

```bash
# JDK 21 (Temurin)
curl -L -o /tmp/jdk21.tar.gz \
  "https://github.com/adoptium/temurin21-binaries/releases/latest/download/OpenJDK21U-jdk_x64_linux_hotspot.tar.gz"
mkdir -p $HOME/jdk21
tar -xzf /tmp/jdk21.tar.gz -C $HOME/jdk21 --strip-components=1

# Android SDK
mkdir -p $HOME/android-sdk/platforms $HOME/android-sdk/build-tools

# Build tools 30.0.3
curl -L -o /tmp/build-tools.zip \
  "https://dl.google.com/android/repository/build-tools_r30.0.3-linux.zip"
unzip -q /tmp/build-tools.zip -d $HOME/android-sdk/build-tools/30.0.3/

# Platform 33 (for aapt2 resource compilation)
curl -L -o /tmp/platform-33.zip \
  "https://dl.google.com/android/repository/platform-33-ext5_r01.zip"
unzip -q /tmp/platform-33.zip -d $HOME/android-sdk/platforms/android-33/

# Platform 36 (for javac compilation target)
curl -L -o /tmp/platform-36.zip \
  "https://dl.google.com/android/repository/platform-36_r01.zip"
unzip -q /tmp/platform-36.zip -d $HOME/android-sdk/platforms/android-36/
```

Persist `JAVA_HOME` in your shell profile:

```bash
echo 'export JAVA_HOME=$HOME/jdk21' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
```

### Termux (Android)

```bash
pkg install openjdk-21 aapt2 dx apksigner curl netcat-openbsd -y

# Download SDKs (same URLs as Manjaro above)
mkdir -p $HOME/android-sdk/platforms
curl -L -o /tmp/platform-33.zip \
  "https://dl.google.com/android/repository/platform-33-ext5_r01.zip"
unzip -q /tmp/platform-33.zip -d $HOME/android-sdk/platforms/android-33/

curl -L -o /tmp/platform-36.zip \
  "https://dl.google.com/android/repository/platform-36_r01.zip"
unzip -q /tmp/platform-36.zip -d $HOME/android-sdk/platforms/android-36/
```

On Termux, `pkg install` places tools in `$PREFIX/bin`. The build script
detects this automatically via the `$PREFIX` env var and uses the correct paths.

## Build

```bash
cd bt-spp-bridge/app
bash build.sh
```

See [Architecture](ARCHITECTURE.md#build-architecture) for the build pipeline
and platform constraints (aapt2 platform-33 requirement).

## Transfer and Install

```bash
# Via adb (USB debug)
adb install build/bt-spp-bridge.apk

# Via SCP (WiFi)
scp -P 2222 build/bt-spp-bridge.apk user@phone:~/storage/downloads/
```

Then open the APK in the file manager on the phone to install.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_HOME` | `$HOME/jdk21` | JDK installation path |
| `ANDROID_HOME` | `$HOME/android-sdk` | Android SDK root |
| `BUILD_TOOLS` | (auto-detected) | Override path to aapt2/dx/apksigner |
| `BUILD_TOOLS_VERSION` | `30.0.3` | Override build-tools version for SDK path |

## Related Docs

- [Architecture](ARCHITECTURE.md) — build pipeline and toolchain
- [Examples](EXAMPLES.md) — usage after installation
