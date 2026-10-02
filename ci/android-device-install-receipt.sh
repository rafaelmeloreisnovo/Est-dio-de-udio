#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_DEVICE_INSTALL][FAIL] %s\n' "$*" >&2
  exit 1
}

apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"
receipt="${2:-android_device_install_receipt_v1.txt}"
package='io.rafaelia.audiostudio'
activity="$package/.MainActivity"

test -f "$apk" || fail "APK_MISSING=$apk"
command -v adb >/dev/null 2>&1 || fail 'adb=TOKEN_VAZIO'
command -v sha256sum >/dev/null 2>&1 || fail 'sha256sum=TOKEN_VAZIO'

if test -n "${ANDROID_SERIAL:-}"; then
  serial="$ANDROID_SERIAL"
  adb_cmd=(adb -s "$serial")
else
  mapfile -t devices < <(adb devices | awk 'NR>1 && $2 == "device" {print $1}')
  test "${#devices[@]}" -eq 1 || fail "CONNECTED_READY_DEVICES=${#devices[@]} expected=1_or_ANDROID_SERIAL"
  serial="${devices[0]}"
  adb_cmd=(adb -s "$serial")
fi

sdk="$("${adb_cmd[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
release="$("${adb_cmd[@]}" shell getprop ro.build.version.release | tr -d '\r')"
model="$("${adb_cmd[@]}" shell getprop ro.product.model | tr -d '\r')"
abilist="$("${adb_cmd[@]}" shell getprop ro.product.cpu.abilist | tr -d '\r')"
serial_sha="$(printf '%s' "$serial" | sha256sum | awk '{print $1}')"
apk_sha="$(sha256sum "$apk" | awk '{print $1}')"

case "$sdk" in
  ''|*[!0-9]*) fail "ANDROID_SDK_INVALID=$sdk" ;;
esac
if test "$sdk" -lt 29; then fail "ANDROID_SDK_TOO_OLD=$sdk"; fi
if test "${RFA_REQUIRE_API29:-0}" = 1 && test "$sdk" -ne 29; then
  fail "ANDROID10_REQUIRED sdk=$sdk"
fi

case ",$abilist," in
  *,armeabi-v7a,*|*,arm64-v8a,*) ;;
  *) fail "DEVICE_ARM_ABI_NOT_FOUND=$abilist" ;;
esac

install_log="$(mktemp)"
launch_log="$(mktemp)"
package_log="$(mktemp)"
trap 'rm -f "$install_log" "$launch_log" "$package_log"' EXIT

if ! "${adb_cmd[@]}" install -r --no-streaming "$apk" >"$install_log" 2>&1; then
  cat "$install_log" >&2
  fail 'ADB_INSTALL=FAIL'
fi
grep -Fq 'Success' "$install_log" || { cat "$install_log" >&2; fail 'ADB_INSTALL_NO_SUCCESS'; }

package_path="$("${adb_cmd[@]}" shell pm path "$package" | tr -d '\r')"
test -n "$package_path" || fail 'PACKAGE_MANAGER_PATH=TOKEN_VAZIO'
printf '%s\n' "$package_path" | grep -Fq 'package:' || fail 'PACKAGE_MANAGER_PATH=FAIL'

"${adb_cmd[@]}" shell dumpsys package "$package" > "$package_log"
version_name="$(grep -m1 'versionName=' "$package_log" | sed 's/^[[:space:]]*//' | tr -d '\r' || true)"
version_code="$(grep -m1 'versionCode=' "$package_log" | sed 's/^[[:space:]]*//' | tr -d '\r' || true)"
primary_abi="$(grep -m1 'primaryCpuAbi=' "$package_log" | sed 's/^[[:space:]]*//' | tr -d '\r' || true)"

if ! "${adb_cmd[@]}" shell am start -W -n "$activity" >"$launch_log" 2>&1; then
  cat "$launch_log" >&2
  fail 'ACTIVITY_LAUNCH=FAIL'
fi
if grep -Fq 'Error:' "$launch_log"; then
  cat "$launch_log" >&2
  fail 'ACTIVITY_LAUNCH_ERROR'
fi
if ! grep -Eq '^Status:[[:space:]]+ok|^Complete$' "$launch_log"; then
  cat "$launch_log" >&2
  fail 'ACTIVITY_LAUNCH_STATUS=UNCONFIRMED'
fi

cat > "$receipt" <<EOF
schema=rafaelia.android-device-install/v1
apk_sha256=$apk_sha
package=$package
activity=$activity
device_serial_sha256=$serial_sha
android_release=$release
android_sdk=$sdk
device_model=$model
device_abilist=$abilist
$version_code
$version_name
$primary_abi
adb_install=PASS
package_manager_presence=PASS
activity_launch=PASS
physical_audio_capture=NOT_RUN
ogg_opus_roundtrip=NOT_RUN
whatsapp_roundtrip=NOT_RUN
cfr_physical=NOT_RUN
external_standard_audit=NOT_AUDITED
claim_allowed=false
EOF

cat "$receipt"
echo "ANDROID_DEVICE_INSTALL_RECEIPT=$receipt"
echo 'INSTALLED_PHYSICAL=PASS'
echo 'LAUNCH_SMOKE=PASS'
