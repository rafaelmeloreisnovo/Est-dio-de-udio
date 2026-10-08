#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dir="app/src/main/java/io/rafaelia/audiostudio"
file="$dir/WavePhysicsLabActivity.java"
test -s "$file"
test -s "$dir/WaveLabCore.java"
grep -Fq 'getScanResults()' "$file"
grep -Fq 'getLastKnownLocation(LocationManager.GPS_PROVIDER)' "$file"
grep -Fq 'new RfaStoredZip(stream)' "$file"
grep -Fq 'TOKEN_VAZIO_MONO_UNDERDETERMINED' "$file"
grep -Fq 'new Intent(this, WavePhysicsLabActivity.class)' "$dir/MainActivity.java"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" "$dir/WaveLabCore.java" ci/WaveLabCoreTest.java
java -cp "$tmp" io.rafaelia.audiostudio.WaveLabCoreTest
javac -encoding UTF-8 -d "$tmp" "$dir/WaveLabDOperators.java" ci/WaveLabDOperatorsTest.java
java -cp "$tmp" io.rafaelia.audiostudio.WaveLabDOperatorsTest

echo 'WAVE_LAB_PLATFORM_FREE_MATH=PASS_HOST'
echo 'WAVE_LAB_UI_ANDROID_COMPILE=NOT_RUN_FAST_LANE'
