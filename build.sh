#!/usr/bin/env bash
set -euo pipefail

SDK="$HOME/android-sdk"
BT="$SDK/build-tools/36.0.0"
PLAT="$SDK/platforms/android-36/android.jar"
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
APK_NAME="GrandBeauty.apk"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$OUT/apk"

echo "[1/6] aapt2 compile"
"$BT/aapt2" compile --dir "$ROOT/res" -o "$OUT/res.zip"

echo "[2/6] aapt2 link"
"$BT/aapt2" link -o "$OUT/base.apk" \
  -I "$PLAT" \
  --manifest "$ROOT/AndroidManifest.xml" \
  --java "$OUT/gen" \
  --version-code 2 --version-name "2.0" \
  --min-sdk-version 24 --target-sdk-version 36 \
  --auto-add-overlay \
  "$OUT/res.zip"

echo "[3/6] javac"
find "$ROOT/src" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -J-Xmx384m --release 11 -nowarn -cp "$PLAT" -d "$OUT/classes" \
  @"$OUT/sources.txt" 2> "$OUT/javac.log" || { cat "$OUT/javac.log"; exit 1; }
[ -s "$OUT/javac.log" ] && cat "$OUT/javac.log"

echo "[4/6] d8"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
# shellcheck disable=SC2046
"$BT/d8" -JXmx700m --release --lib "$PLAT" --min-api 24 \
  --output "$OUT/dex" $(cat "$OUT/classes.txt")

echo "[5/6] package + align"
cp "$OUT/base.apk" "$OUT/apk/app.apk"
(cd "$OUT/dex" && zip -q "$OUT/apk/app.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/apk/app.apk" "$OUT/apk/app-aligned.apk"

echo "[6/6] sign"
KS="$HOME/.config/grandbeauty/keystore.jks"
PASSFILE="$HOME/.config/grandbeauty/signing"
mkdir -p "$HOME/.config/grandbeauty"
if [ ! -f "$KS" ]; then
  pass="$(head -c 24 /dev/urandom | base64 | tr -d '\n')"
  printf '%s' "$pass" > "$PASSFILE"
  chmod 600 "$PASSFILE"
  keytool -genkeypair -keystore "$KS" -alias grandbeauty \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$pass" -keypass "$pass" \
    -dname "CN=Grand Beauty, O=Grand Beauty, C=KE"
  chmod 600 "$KS"
fi
PASS="$(cat "$PASSFILE")"
"$BT/apksigner" sign \
  --ks "$KS" --ks-key-alias grandbeauty \
  --ks-pass "pass:$PASS" --key-pass "pass:$PASS" \
  --out "$ROOT/$APK_NAME" "$OUT/apk/app-aligned.apk"

"$BT/apksigner" verify --print-certs "$ROOT/$APK_NAME" | head -3
ls -lh "$ROOT/$APK_NAME"
echo "OK -> $ROOT/$APK_NAME"
