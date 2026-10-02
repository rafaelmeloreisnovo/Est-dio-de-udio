#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_INSTALLABILITY][FAIL] %s\n' "$*" >&2
  exit 1
}

apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"
test -f "$apk" || fail "APK_MISSING=$apk"
test -n "${ANDROID_SDK_ROOT:-}" || fail 'ANDROID_SDK_ROOT=TOKEN_VAZIO'
command -v unzip >/dev/null 2>&1 || fail 'unzip=TOKEN_VAZIO'

build_tools="$({ find "$ANDROID_SDK_ROOT/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null || true; } | LC_ALL=C sort -V | tail -n 1)"
test -n "$build_tools" || fail 'ANDROID_BUILD_TOOLS=TOKEN_VAZIO'

apksigner="$build_tools/apksigner"
zipalign="$build_tools/zipalign"
aapt="$build_tools/aapt"

test -x "$apksigner" || fail "APKSIGNER_MISSING=$apksigner"
test -x "$zipalign" || fail "ZIPALIGN_MISSING=$zipalign"
test -x "$aapt" || fail "AAPT_MISSING=$aapt"

unzip -tqq "$apk" || fail 'APK_ZIP_INTEGRITY=FAIL'

"$apksigner" verify --verbose --print-certs "$apk" > apk-signature-report.txt || {
  cat apk-signature-report.txt >&2 || true
  fail 'APK_SIGNATURE_VERIFY=FAIL'
}

"$zipalign" -c -P 16 -v 4 "$apk" > apk-zipalign-report.txt || {
  cat apk-zipalign-report.txt >&2 || true
  fail 'APK_ZIPALIGN=FAIL'
}

"$aapt" dump badging "$apk" > apk-badging.txt || fail 'APK_BADGING=FAIL'
grep -Fq "package: name='io.rafaelia.audiostudio'" apk-badging.txt || fail 'APK_PACKAGE_ID=FAIL'
grep -Fq "sdkVersion:'29'" apk-badging.txt || fail 'APK_MIN_SDK=FAIL'
grep -Fq "targetSdkVersion:'36'" apk-badging.txt || fail 'APK_TARGET_SDK=FAIL'

unzip -Z1 "$apk" > apk-entry-list.txt
grep -Fxq 'lib/armeabi-v7a/librafaelia_audio.so' apk-entry-list.txt || fail 'APK_ARMV7_LIB=FAIL'
grep -Fxq 'lib/arm64-v8a/librafaelia_audio.so' apk-entry-list.txt || fail 'APK_ARM64_LIB=FAIL'

apk_sha="$(sha256sum "$apk" | awk '{print $1}')"
cert_line="$(grep -m1 'Signer #1 certificate SHA-256 digest:' apk-signature-report.txt || true)"

echo "APK_SHA256=$apk_sha"
echo 'APK_ZIP_INTEGRITY=PASS'
echo 'APK_SIGNATURE_VERIFY=PASS'
echo 'APK_ZIPALIGN=PASS'
echo 'APK_PACKAGE_ID=io.rafaelia.audiostudio'
echo 'APK_MIN_SDK=29'
echo 'APK_TARGET_SDK=36'
echo 'APK_ABIS=armeabi-v7a,arm64-v8a'
printf 'APK_CERT=%s\n' "${cert_line:-AVAILABLE_IN_REPORT}"
echo 'INSTALLABLE_STATIC=PASS'
echo 'INSTALLED_PHYSICAL=NOT_RUN'

bash ci/test-android-device-install-receipt.sh
