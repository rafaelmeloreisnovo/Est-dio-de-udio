#!/usr/bin/env bash
# SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sut="$repo_root/ci/android-device-install-receipt.sh"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

good_apk="$tmp/good.apk"
bad_apk="$tmp/bad.apk"
printf 'rafaelia-exact-apk-v1\n' > "$good_apk"
printf 'different-installed-bytes\n' > "$bad_apk"

good_sha="$(sha256sum "$good_apk" | awk '{print $1}')"
signer_sha="$(printf 'ab%.0s' {1..32})"
source_sha="$(printf '1%.0s' {1..40})"

cat > "$tmp/adb" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if test "${1:-}" = devices; then
  printf 'List of devices attached\nFAKE123\tdevice\n'
  exit 0
fi
if test "${1:-}" = -s; then
  shift 2
fi
case "${1:-}:${2:-}:${3:-}" in
  shell:getprop:ro.build.version.sdk) printf '29\n' ;;
  shell:getprop:ro.build.version.release) printf '10\n' ;;
  shell:getprop:ro.product.model) printf 'moto e(7) power\n' ;;
  shell:getprop:ro.product.cpu.abilist) printf 'armeabi-v7a,arm64-v8a\n' ;;
  install:-r:--no-streaming) printf 'Success\n' ;;
  shell:pm:path) printf 'package:/data/app/fake/base.apk\n' ;;
  shell:dumpsys:package)
    printf '  versionCode=1 minSdk=29 targetSdk=35\n'
    printf '  versionName=1.0\n'
    printf '  primaryCpuAbi=armeabi-v7a\n'
    ;;
  shell:am:start)
    printf 'Status: ok\nComplete\n'
    ;;
  exec-out:cat:/data/app/fake/base.apk)
    cat "$FAKE_INSTALLED_APK"
    ;;
  *)
    printf 'unexpected fake adb invocation:' >&2
    printf ' <%s>' "$@" >&2
    printf '\n' >&2
    exit 97
    ;;
esac
EOF
chmod +x "$tmp/adb"

cat > "$tmp/apksigner" <<EOF
#!/usr/bin/env bash
set -euo pipefail
printf 'Signer #1 certificate SHA-256 digest: %s\n' '$signer_sha'
EOF
chmod +x "$tmp/apksigner"

receipt="$tmp/receipt.txt"
PATH="$tmp:$PATH" \
ADB_BIN="$tmp/adb" \
APKSIGNER_BIN="$tmp/apksigner" \
ANDROID_SERIAL=FAKE123 \
FAKE_INSTALLED_APK="$good_apk" \
RFA_EXPECTED_APK_SHA256="$good_sha" \
RFA_SOURCE_SHA="$source_sha" \
RFA_CI_RUN_ID=37017383796 \
RFA_EXPECTED_CERT_SHA256=TOKEN_VAZIO \
bash "$sut" "$good_apk" "$receipt" > "$tmp/pass.log"

grep -Fq 'schema=rafaelia.android-device-install/v2' "$receipt"
grep -Fq "source_sha=$source_sha" "$receipt"
grep -Fq 'ci_run_id=37017383796' "$receipt"
grep -Fq "expected_apk_sha256=$good_sha" "$receipt"
grep -Fq "local_apk_sha256=$good_sha" "$receipt"
grep -Fq "installed_base_apk_sha256=$good_sha" "$receipt"
grep -Fq "signer_cert_sha256=$signer_sha" "$receipt"
grep -Fq 'local_expected_byte_identity=PASS' "$receipt"
grep -Fq 'installed_expected_byte_identity=PASS' "$receipt"
grep -Fq 'claim_allowed=false' "$receipt"

if PATH="$tmp:$PATH" \
  ADB_BIN="$tmp/adb" \
  APKSIGNER_BIN="$tmp/apksigner" \
  ANDROID_SERIAL=FAKE123 \
  FAKE_INSTALLED_APK="$good_apk" \
  RFA_EXPECTED_APK_SHA256="$(printf '0%.0s' {1..64})" \
  RFA_SOURCE_SHA="$source_sha" \
  RFA_CI_RUN_ID=37017383796 \
  bash "$sut" "$good_apk" "$tmp/negative-local.txt" >"$tmp/negative-local.log" 2>&1; then
  echo 'EXPECTED_LOCAL_SHA_MISMATCH=FAIL'
  exit 1
fi
grep -Fq 'LOCAL_APK_SHA_MISMATCH' "$tmp/negative-local.log"

if PATH="$tmp:$PATH" \
  ADB_BIN="$tmp/adb" \
  APKSIGNER_BIN="$tmp/apksigner" \
  ANDROID_SERIAL=FAKE123 \
  FAKE_INSTALLED_APK="$bad_apk" \
  RFA_EXPECTED_APK_SHA256="$good_sha" \
  RFA_SOURCE_SHA="$source_sha" \
  RFA_CI_RUN_ID=37017383796 \
  bash "$sut" "$good_apk" "$tmp/negative-installed.txt" >"$tmp/negative-installed.log" 2>&1; then
  echo 'EXPECTED_INSTALLED_SHA_MISMATCH=FAIL'
  exit 1
fi
grep -Fq 'INSTALLED_APK_SHA_MISMATCH' "$tmp/negative-installed.log"

echo 'DEVICE_INSTALL_RECEIPT_CONTRACT=PASS'
echo 'PHYSICAL_DEVICE_EXECUTION=NOT_RUN'
echo 'CLAIM_ALLOWED=false'
