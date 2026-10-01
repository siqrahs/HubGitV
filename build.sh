#!/usr/bin/env bash
# Build APK HubGitV (klien GitHub Android, Kotlin + Compose) TANPA Gradle.
#
# Di perangkat ini tidak ada Android SDK/Gradle daemon yang layak, tapi
# toolchain yang dibutuhkan (kotlinc, aapt2, d8, apksigner, JDK) semuanya ada.
# Build satu tahap, deterministik, dan cepat.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRATCH="${HARNESS_SCRATCH:?HARNESS_SCRATCH tidak diset}"
LIBS="$ROOT/libs"
SDK="$SCRATCH/sdk"
JDK="$PREFIX/lib/jvm/java-17-openjdk/bin"
AAPT2="$(command -v aapt2 || echo "$PREFIX/bin/aapt2")"
D8="$SDK/bt/android-14/d8"
APKSIGNER="$SDK/bt/android-14/apksigner"
ANDROID_JAR="$SDK/pl/android-34/android.jar"
KOTLINC="$SCRATCH/kt24/kotlinc/bin/kotlinc"
KOTLIN_LIB="$SCRATCH/kt24/kotlinc/lib"

SRC="$ROOT/src"
RES="$ROOT/res"
BUILD="$ROOT/build"
PKG="com/hubgitv/client"

export PATH="$JDK:$PATH"

for t in "$AAPT2" "$D8" "$APKSIGNER" "$ANDROID_JAR" "$JDK/javac" "$KOTLINC"; do
  [ -e "$t" ] || { echo "HILANG: $t" >&2; exit 1; }
done

rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$BUILD/res"

echo "==> extract classes.jar dari AAR"
X="$BUILD/x"
mkdir -p "$X"
for a in "$LIBS"/*.aar; do
  n="$(basename "$a" .aar)"
  mkdir -p "$X/$n"
  unzip -oq "$a" classes.jar -d "$X/$n"
done
CP="$ANDROID_JAR"
for j in "$LIBS"/*.jar; do CP="$CP:$j"; done
for j in "$X"/*/classes.jar; do CP="$CP:$j"; done

echo "==> kompilasi Kotlin"
"$KOTLINC" -nowarn -jvm-target 17 -no-reflect \
  -Xjvm-default=all \
  -classpath "$CP" \
  -d "$BUILD/classes" \
  $(find "$SRC" -name '*.kt')

echo "==> link resources (aapt2)"
"$AAPT2" compile --dir "$RES" -o "$BUILD/res.zip"
"$AAPT2" link \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -R "$BUILD/res.zip" \
  --java "$BUILD/gen" \
  --min-sdk-version 24 --target-sdk-version 34 \
  --version-code 1 --version-name 1 \
  -o "$BUILD/base.apk"
if [ -d "$BUILD/gen" ]; then
  "$KOTLINC" -nowarn -jvm-target 17 \
    -classpath "$CP:$BUILD/classes" \
    -d "$BUILD/classes" \
    $(find "$BUILD/gen" -name '*.java')
fi

echo "==> dex"
D8_ARGS=()
for j in "$KOTLIN_LIB/kotlin-stdlib.jar" "$LIBS"/*.jar; do D8_ARGS+=("$j"); done
for j in "$X"/*/classes.jar; do D8_ARGS+=("$j"); done
"$D8" --release --min-api 24 --lib "$ANDROID_JAR" \
  --output "$BUILD/dex" \
  --classpath "$KOTLIN_LIB/kotlin-stdlib.jar" \
  $(find "$BUILD/classes" -name '*.class') \
  "${D8_ARGS[@]}"

echo "==> pack APK"
python3 - "$BUILD" <<'PY'
import os, sys, zipfile
build = sys.argv[1]
out = os.path.join(build, "unsigned.apk")
dexes = []
for root, _, files in os.walk(os.path.join(build, "dex")):
    for f in files:
        if f.endswith(".dex"):
            dexes.append(os.path.join(root, f))
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for name in ("resources.arsc", "AndroidManifest.xml"):
        p = os.path.join(build, "base.apk", name)
        if os.path.exists(p):
            z.write(p, name)
    for d in dexes:
        z.write(d, os.path.relpath(d, build).replace(os.sep, "/").split("/")[1])
    # classes pulled straight from the aapt2 base apk (res only, so normally empty)
    with zipfile.ZipFile(os.path.join(build, "base.apk")) as src:
        for n in src.namelist():
            if n.startswith("res/") or n.startswith("assets/"):
                z.writestr(n, src.read(n))
    print("   +", len(z.namelist()), "entries")
PY

echo "==> tanda tangan"
KS="$ROOT/keystore.jks"
if [ ! -f "$KS" ]; then
  "$JDK/keytool" -genkeypair -v -keystore "$KS" -storepass androidx \
    -keypass androidx -alias app -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=HubGitV, OU=Client, O=hubgitv, C=ID" >/dev/null 2>&1
fi
"$APKSIGNER" sign \
  --ks "$KS" --ks-pass pass:androidx --key-pass pass:androidx \
  --out "$ROOT/HubGitV.apk" "$BUILD/unsigned.apk"

echo "==> selesai: $ROOT/HubGitV.apk"
ls -la "$ROOT/HubGitV.apk"
