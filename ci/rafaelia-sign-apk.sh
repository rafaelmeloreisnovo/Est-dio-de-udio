#!/usr/bin/env bash
set -euo pipefail

unsigned_apk="${1:-}"
signed_apk="${2:-}"
receipt="${3:-}"

die() {
  printf 'RAFAELIA_SIGN_FAIL=%s\n' "$1" >&2
  exit 1
}

[ -n "$unsigned_apk" ] || die "UNSIGNED_APK_ARG_TOKEN_VAZIO"
[ -n "$signed_apk" ] || die "SIGNED_APK_ARG_TOKEN_VAZIO"
[ -n "$receipt" ] || die "RECEIPT_ARG_TOKEN_VAZIO"
[ -f "$unsigned_apk" ] || die "UNSIGNED_APK_NOT_FOUND"

: "${RFA_ANDROID_KEYSTORE_B64:?RAFAELIA_ANDROID_KEYSTORE_B64_TOKEN_VAZIO}"
: "${RFA_ANDROID_STORE_PASSWORD:?RAFAELIA_ANDROID_STORE_PASSWORD_TOKEN_VAZIO}"
: "${RFA_ANDROID_KEY_ALIAS:?RAFAELIA_ANDROID_KEY_ALIAS_TOKEN_VAZIO}"
: "${RFA_ANDROID_KEY_PASSWORD:?RAFAELIA_ANDROID_KEY_PASSWORD_TOKEN_VAZIO}"
: "${RFA_EXPECTED_CERT_SHA256:?RAFAELIA_EXPECTED_CERT_SHA256_TOKEN_VAZIO}"
: "${RFA_SIGNER_ID:?RAFAELIA_SIGNER_ID_TOKEN_VAZIO}"

build_tools="${ANDROID_SDK_ROOT:?ANDROID_SDK_ROOT_TOKEN_VAZIO}/build-tools/36.0.0"
apksigner="$build_tools/apksigner"
[ -x "$apksigner" ] || die "APKSIGNER_NOT_FOUND"

tmp="${RUNNER_TEMP:-/tmp}/rafaelia-sign-$$"
mkdir -p "$tmp"
trap 'rm -rf "$tmp"' EXIT
ks="$tmp/rafaelia-signing.keystore"

printf '%s' "$RFA_ANDROID_KEYSTORE_B64" | base64 --decode > "$ks"
[ -s "$ks" ] || die "KEYSTORE_DECODE_EMPTY"

export RFA_KS_PASS_INTERNAL="$RFA_ANDROID_STORE_PASSWORD"
export RFA_KEY_PASS_INTERNAL="$RFA_ANDROID_KEY_PASSWORD"

"$apksigner" sign \
  --ks "$ks" \
  --ks-key-alias "$RFA_ANDROID_KEY_ALIAS" \
  --ks-pass env:RFA_KS_PASS_INTERNAL \
  --key-pass env:RFA_KEY_PASS_INTERNAL \
  --out "$signed_apk" \
  "$unsigned_apk"

verify_out="$tmp/apksigner.verify.txt"
"$apksigner" verify --verbose --print-certs "$signed_apk" > "$verify_out"

cert="$(
  awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print $2; exit}' "$verify_out" |
  tr -d ':[:space:]' |
  tr 'A-F' 'a-f'
)"
expected="$(
  printf '%s' "$RFA_EXPECTED_CERT_SHA256" |
  tr -d ':[:space:]' |
  tr 'A-F' 'a-f'
)"

[ "${#cert}" -eq 64 ] || die "CERT_SHA256_PARSE_FAIL"
[ "${#expected}" -eq 64 ] || die "EXPECTED_CERT_SHA256_INVALID"
[ "$cert" = "$expected" ] || die "CERT_SHA256_MISMATCH"

bash ci/apk-installability-static.sh "$signed_apk"

apk_sha="$(sha256sum "$signed_apk" | awk '{print $1}')"
unsigned_sha="$(sha256sum "$unsigned_apk" | awk '{print $1}')"

cat > "$receipt" <<EOF
schema=rafaelia.signed-release/v2
source_sha=${RFA_SOURCE_SHA:-UNAVAILABLE}
run_id=${RFA_CI_RUN_ID:-UNAVAILABLE}
run_number=${RFA_CI_RUN_NUMBER:-UNAVAILABLE}
version_code=${RFA_VERSION_CODE:-TOKEN_VAZIO}
version_name=${RFA_VERSION_NAME:-TOKEN_VAZIO}
signer_id=$RFA_SIGNER_ID
android_key_alias=$RFA_ANDROID_KEY_ALIAS
expected_certificate_sha256=$expected
observed_certificate_sha256=$cert
certificate_match=PASS
unsigned_apk_sha256=$unsigned_sha
signed_apk_sha256=$apk_sha
signature_tool=Android_build_tools_36.0.0_apksigner
installable_static=PASS
installed_physical=NOT_RUN
private_key_material=SECRET_NOT_EXPORTED
EOF

printf 'RFA_SIGNED_APK_SHA256=%s\n' "$apk_sha"
printf 'RFA_SIGNING_CERT_SHA256=%s\n' "$cert"
printf 'RFA_SIGN_RESULT=PASS\n'
