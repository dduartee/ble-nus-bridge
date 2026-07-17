#!/bin/bash
# ==============================================
# BUILD: BLE NUS Bridge APK
# Compatible: Manjaro Linux + Termux (Android)
# ==============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"

# ── SDK/JDK paths ─────────────────────────────────────────
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk21}"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-30.0.3}"

if [ -n "$PREFIX" ]; then
    BUILD_TOOLS="${BUILD_TOOLS:-$PREFIX/bin}"
else
    BUILD_TOOLS="${BUILD_TOOLS:-$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION}"
fi

PLATFORM_33="$ANDROID_HOME/platforms/android-33"
PLATFORM_36="$ANDROID_HOME/platforms/android-36"

AAPT2="$BUILD_TOOLS/aapt2"
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

[ -d "$JAVA_HOME" ]  || fail "JDK not found at $JAVA_HOME"
[ -d "$ANDROID_HOME" ] || fail "Android SDK not found at $ANDROID_HOME"
[ -f "$AAPT2" ]      || fail "aapt2 not found at $AAPT2"
[ -f "$APKSIGNER" ]  || fail "apksigner not found at $APKSIGNER"
[ -f "$ANDROID_JAR_36" ] || fail "android.jar (API 36) not found at $ANDROID_JAR_36"
[ -f "$ANDROID_JAR_33" ] || fail "android.jar (API 33) not found at $ANDROID_JAR_33"
[ -f "$DX" ]            || fail "dx not found at $DX"

echo "🔨 Build $APP_NAME"
echo "   JAVA_HOME=$JAVA_HOME (v$($JAVA_HOME/bin/java -Xinternalversion 2>&1 | head -1))"
echo "   SDK 33: $ANDROID_JAR_33"
echo "   SDK 36: $ANDROID_JAR_36"
echo ""

# ── Clean ─────────────────────────────────────────────────
rm -rf "$BUILD_DIR"
mkdir -p "$APK_DIR" "$BUILD_DIR/classes" "$BUILD_DIR/R"

# ── Step 1: Ensure strings.xml exists ─────────────────────
if [ ! -f src/main/res/values/strings.xml ]; then
    mkdir -p src/main/res/values
    echo '<?xml version="1.0" encoding="utf-8"?><resources><string name="app_name">BT SPP Bridge</string></resources>' > src/main/res/values/strings.xml
fi

# ── Step 2: aapt2 compile resources ───────────────────────
echo "📦 [1/5] aapt2 compile..."
$AAPT2 compile --dir src/main/res -o "$BUILD_DIR/compiled.flata" 2>&1 | tail -1

# ── Step 3: aapt2 link (platform-33) ──────────────────────
echo "📦 [2/5] aapt2 link (platform-33)..."
$AAPT2 link \
    -o "$APK_DIR/base.apk" \
    -I "$ANDROID_JAR_33" \
    --manifest src/main/AndroidManifest.xml \
    --java "$BUILD_DIR/R" \
    --min-sdk-version 26 \
    --target-sdk-version 33 \
    --version-code 1 \
    --version-name "1.0" \
    "$BUILD_DIR/compiled.flata" 2>&1 | tail -1

# ── Step 4: Compile Java (platform-36) ────────────────────
echo "☕ [3/5] javac (platform-36)..."
JAVA_SRC=$(find src/main/java -name "*.java" 2>/dev/null)
R_JAVA=$(find "$BUILD_DIR/R" -name "*.java" 2>/dev/null)

$JAVA_HOME/bin/javac \
    -source 8 \
    -target 8 \
    -cp "$ANDROID_JAR_36" \
    -d "$BUILD_DIR/classes" \
    $JAVA_SRC $R_JAVA

CLASS_COUNT=$(find "$BUILD_DIR/classes" -name "*.class" | wc -l)
echo "   $CLASS_COUNT classes compiled"

# ── Step 5: Convert to DEX (dx) ───────────────────────────
echo "📱 [4/5] dx..."
$DX --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes"

cp "$BUILD_DIR/classes.dex" "$APK_DIR/"
(cd "$APK_DIR" && zip -r base.apk classes.dex >/dev/null 2>&1); cd "$PROJECT_DIR"

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
echo "✅ BUILD SUCCESS!"
echo "   APK: $BUILD_DIR/$APP_NAME.apk"
echo "   $(du -h "$BUILD_DIR/$APP_NAME.apk" | cut -f1)"
echo "============================================="
echo ""
echo "📲 Install:"
echo "   adb install $BUILD_DIR/$APP_NAME.apk"
echo "   (or transfer to device via SCP/USB)"
