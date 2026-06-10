#!/bin/bash
# ==============================================
# BUILD: BT SPP Bridge APK (Manjaro Linux)
# Ferramentas:
#   - JDK 21 (Temurin) no $HOME/jdk21
#   - Android SDK no $HOME/android-sdk
#   - aapt2 + apksigner do build-tools 34.0.0
#   - dx + aapt2 + apksigner do build-tools 30.0.3
# ==============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"

# ── SDK/JDK paths ─────────────────────────────────────────
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk21}"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"

BUILD_TOOLS="$ANDROID_HOME/build-tools/30.0.3"
PLATFORM_36="$ANDROID_HOME/platforms/android-36"
PLATFORM_33="$ANDROID_HOME/platforms/android-33"

AAPT2="$BUILD_TOOLS/aapt2"
AAPT="$BUILD_TOOLS/aapt"
DX="$BUILD_TOOLS/dx"
APKSIGNER="$BUILD_TOOLS/apksigner"

ANDROID_JAR_36="$PLATFORM_36/android.jar"
ANDROID_JAR_33="$PLATFORM_33/android.jar"

# ── Project ───────────────────────────────────────────────
APP_NAME="bt-spp-bridge"
BUILD_DIR="$PROJECT_DIR/build"
APK_DIR="$BUILD_DIR/apk"

cd "$PROJECT_DIR"

# ── Verify environment ────────────────────────────────────
fail() { echo "❌ $1"; exit 1; }

[ -d "$JAVA_HOME" ]  || fail "JDK não encontrado em $JAVA_HOME"
[ -d "$ANDROID_HOME" ] || fail "Android SDK não encontrado em $ANDROID_HOME"
[ -f "$AAPT2" ]      || fail "aapt2 não encontrado em $AAPT2"
[ -f "$APKSIGNER" ]  || fail "apksigner não encontrado em $APKSIGNER"
[ -f "$ANDROID_JAR_36" ] || fail "android.jar (API 36) não encontrado em $ANDROID_JAR_36"
[ -f "$ANDROID_JAR_33" ] || fail "android.jar (API 33) não encontrado em $ANDROID_JAR_33"
[ -f "$DX" ]            || fail "dx não encontrado em $DX"

echo "🔨 Build $APP_NAME"
echo "   JAVA_HOME=$JAVA_HOME (v$($JAVA_HOME/bin/java -Xinternalversion 2>&1 | head -1))"
echo "   SDK 33: $ANDROID_JAR_33"
echo "   SDK 36: $ANDROID_JAR_36"
echo ""

# ── Clean ─────────────────────────────────────────────────
rm -rf "$BUILD_DIR"
mkdir -p "$APK_DIR" "$BUILD_DIR/classes" "$BUILD_DIR/R"

# ── Step 1: Ensure strings.xml exists ─────────────────────
if [ ! -f app/src/main/res/values/strings.xml ]; then
    mkdir -p app/src/main/res/values
    echo '<?xml version="1.0" encoding="utf-8"?><resources><string name="app_name">BT SPP Bridge</string></resources>' > app/src/main/res/values/strings.xml
fi

# ── Step 2: aapt2 compile resources ───────────────────────
echo "📦 [1/5] aapt2 compile..."
$AAPT2 compile --dir app/src/main/res -o "$BUILD_DIR/compiled.flata" 2>&1 | tail -1

# ── Step 3: aapt2 link (platform-33) ──────────────────────
echo "📦 [2/5] aapt2 link (platform-33)..."
$AAPT2 link \
    -o "$APK_DIR/base.apk" \
    -I "$ANDROID_JAR_33" \
    --manifest app/src/main/AndroidManifest.xml \
    --java "$BUILD_DIR/R" \
    --min-sdk-version 26 \
    --target-sdk-version 33 \
    --version-code 1 \
    --version-name "1.0" \
    "$BUILD_DIR/compiled.flata" 2>&1 | tail -1

# ── Step 4: Compile Java (platform-36) ────────────────────
echo "☕ [3/5] javac (platform-36)..."
JAVA_SRC=$(find app/src/main/java -name "*.java" 2>/dev/null)
R_JAVA=$(find "$BUILD_DIR/R" -name "*.java" 2>/dev/null)

$JAVA_HOME/bin/javac \
    -source 8 \
    -target 8 \
    -cp "$ANDROID_JAR_36" \
    -d "$BUILD_DIR/classes" \
    $JAVA_SRC $R_JAVA

CLASS_COUNT=$(find "$BUILD_DIR/classes" -name "*.class" | wc -l)
echo "   $CLASS_COUNT classes compiladas"

# ── Step 5: Convert to DEX (dx) ───────────────────────────
echo "📱 [4/5] dx..."
$DX --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes"

cp "$BUILD_DIR/classes.dex" "$APK_DIR/"
cd "$APK_DIR" && $AAPT add base.apk classes.dex >/dev/null 2>&1; cd "$PROJECT_DIR"

# ── Step 6: Sign APK ──────────────────────────────────────
echo "✍️  [5/5] apksigner..."
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"
DEBUG_ALIAS="androiddebugkey"
DEBUG_PASS="android"

if [ ! -f "$DEBUG_KEYSTORE" ]; then
    mkdir -p "$HOME/.android"
    keytool -genkey -v \
        -keystore "$DEBUG_KEYSTORE" \
        -alias "$DEBUG_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "$DEBUG_PASS" -keypass "$DEBUG_PASS" \
        -dname "CN=Debug, OU=Dev, O=BTBridge, L=Unknown, ST=Unknown, C=BR"
fi

$APKSIGNER sign \
    --ks "$DEBUG_KEYSTORE" \
    --ks-pass pass:"$DEBUG_PASS" \
    --ks-key-alias "$DEBUG_ALIAS" \
    --out "$BUILD_DIR/$APP_NAME.apk" \
    "$APK_DIR/base.apk"

echo ""
echo "============================================="
echo "✅ BUILD SUCESSO!"
echo "   APK: $BUILD_DIR/$APP_NAME.apk"
echo "   $(du -h "$BUILD_DIR/$APP_NAME.apk" | cut -f1)"
echo "============================================="
echo ""
echo "📲 Instalar:"
echo "   adb install $BUILD_DIR/$APP_NAME.apk"
echo "   (ou transfira para o S23 via SCP/USB)"
