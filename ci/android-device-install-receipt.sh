#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_DEVICE_INSTALL][FAIL] %s\n' "$*" >&2
  exit 1
}

normalize_hex() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]'
}

apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"
receipt="${2:-android_device_install_receipt_v2.txt}"
package='io.rafaelia.audiostudio'
activity="$package/.MainActivity"
adb_bin="${ADB_BIN:-adb}"
apksigner_bin="${APKSIGNER_BIN:-apksigner}"

expected_apk_sha="$(normalize_hex "${RFA_EXPECTED_APK_SHA256:-}")"
source_sha="$(normalize_hex "${RFA_SOURCE_SHA:-}")"
ci_run_id="${RFA_CI_RUN_ID:-}"
expected_cert_sha="${RFA_EXPECTED_CERT_SHA256:-TOKEN_VAZIO}"

test -f "$apk" || fail "APK_MISSING=$apk"
command -v "$adb_bin" >/dev/null 2>&1 || fail 'adb=TOKEN_VAZIO'
command -v "$apksigner_bin" >/dev/null 2>&1 || fail 'apksigner=TOKEN_VAZIO'
command -v sha256sum >/dev/null 2>&1 || fail 'sha256sum=TOKEN_VAZIO'

[[ "$expected_apk_sha" =~ ^[0-9a-f]{64}$ ]] || fail 'EXPECTED_APK_SHA256=TOKEN_VAZIO_OR_INVALID'
[[ "$source_sha" =~ ^[0-9a-f]{40}$ ]] || fail 'SOURCE_SHA=TOKEN_VAZIO_OR_INVALID'
[[ "$ci_run_id" =~ ^[0-9]+$ ]] || fail 'CI_RUN_ID=TOKEN_VAZIO_OR_INVALID'

apk_sha="$(sha256sum "$apk" | awk '{print $1}')"
test "$apk_sha" = "$expected_apk_sha" ||
  fail "LOCAL_APK_SHA_MISMATCH expected=$expected_apk_sha actual=$apk_sha"

signer_cert_sha="$(
  "$apksigner_bin" verify --print-certs "$apk" |
    sed -n 's/^Signer #1 certificate SHA-256 digest: //p' |
    head -n1 |
    tr '[:upper:]' '[:lower:]'
)"
[[ "$signer_cert_sha" =~ ^[0-9a-f]{64}$ ]] || fail 'SIGNER_CERT_SHA256=TOKEN_VAZIO_OR_INVALID'

if test "$expected_cert_sha" != TOKEN_VAZIO && test -n "$expected_cert_sha"; then
  expected_cert_sha="$(normalize_hex "$expected_cert_sha")"
  [[ "$expected_cert_sha" =~ ^[0-9a-f]{64}$ ]] || fail 'EXPECTED_CERT_SHA256=INVALID'
  test "$signer_cert_sha" = "$expected_cert_sha" ||
    fail "SIGNER_CERT_MISMATCH expected=$expected_cert_sha actual=$signer_cert_sha"
fi

if test -n "${ANDROID_SERIAL:-}"; then
  serial="$ANDROID_SERIAL"
  adb_cmd=("$adb_bin" -s "$serial")
else
  mapfile -t devices < <("$adb_bin" devices | awk 'NR>1 && $2 == "device" {print $1}')
  test "${#devices[@]}" -eq 1 ||
    fail "CONNECTED_READY_DEVICES=${#devices[@]} expected=1_or_ANDROID_SERIAL"
  serial="${devices[0]}"
  adb_cmd=("$adb_bin" -s "$serial")
fi

sdk="$("${adb_cmd[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
release="$("${adb_cmd[@]}" shell getprop ro.build.version.release | tr -d '\r')"
model="$("${adb_cmd[@]}" shell getprop ro.product.model | tr -d '\r')"
abilist="$("${adb_cmd[@]}" shell getprop ro.product.cpu.abilist | tr -d '\r')"
serial_sha="$(printf '%s' "$serial" | sha256sum | awk '{print $1}')"

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
grep -Fq 'Success' "$install_log" ||
  { cat "$install_log" >&2; fail 'ADB_INSTALL_NO_SUCCESS'; }

package_paths="$("${adb_cmd[@]}" shell pm path "$package" | tr -d '\r')"
test -n "$package_paths" || fail 'PACKAGE_MANAGER_PATH=TOKEN_VAZIO'
printf '%s\n' "$package_paths" | grep -Fq 'package:' || fail 'PACKAGE_MANAGER_PATH=FAIL'

remote_apk_path="$(
  printf '%s\n' "$package_paths" |
    sed -n 's/^package://p' |
    awk '/\/base\.apk$/ {print; found=1; exit} END {if (!found) exit 1}'
)" || remote_apk_path=''
if test -z "$remote_apk_path"; then
  remote_apk_path="$(printf '%s\n' "$package_paths" | sed -n 's/^package://p' | head -n1)"
fi
test -n "$remote_apk_path" || fail 'INSTALLED_BASE_APK_PATH=TOKEN_VAZIO'

installed_apk_sha="$("${adb_cmd[@]}" exec-out cat "$remote_apk_path" | sha256sum | awk '{print $1}')"
test "$installed_apk_sha" = "$expected_apk_sha" ||
  fail "INSTALLED_APK_SHA_MISMATCH expected=$expected_apk_sha actual=$installed_apk_sha"

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
schema=rafaelia.android-device-install/v2
source_sha=$source_sha
ci_run_id=$ci_run_id
expected_apk_sha256=$expected_apk_sha
local_apk_sha256=$apk_sha
installed_base_apk_sha256=$installed_apk_sha
signer_cert_sha256=$signer_cert_sha
expected_cert_sha256=$expected_cert_sha
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
local_expected_byte_identity=PASS
installed_expected_byte_identity=PASS
adb_install=PASS
package_manager_presence=PASS
activity_launch=PASS
physical_audio_capture=NOT_RUN
sensor_vibration_evidence=NOT_RUN
sensor_magnetic=TOKEN_VAZIO_NOT_OBSERVED
zipraf_current_head=NOT_RUN
ogg_opus_roundtrip=NOT_RUN
whatsapp_roundtrip=NOT_RUN
cfr_physical=NOT_RUN
absolute_spl=PENDING_PHYSICAL_REFERENCE
independent_reproduction=NOT_CLAIMED
external_standard_audit=NOT_AUDITED
claim_allowed=false
EOF

cat "$receipt"
echo "ANDROID_DEVICE_INSTALL_RECEIPT=$receipt"
echo 'INSTALLED_PHYSICAL=PASS_EXACT_BYTES'
echo 'LAUNCH_SMOKE=PASS'
