#!/usr/bin/env bash
set -euo pipefail

unsigned_aab="${1:-}"
signed_aab="${2:-}"
receipt="${3:-}"

fail() {
  printf '[RAFAELIA_AAB_SIGN][FAIL] %s\n' "$*" >&2
  exit 1
}

[ -f "$unsigned_aab" ] || fail 'UNSIGNED_AAB_NOT_FOUND'
[ -n "$signed_aab" ] || fail 'SIGNED_AAB_ARG_TOKEN_VAZIO'
[ -n "$receipt" ] || fail 'RECEIPT_ARG_TOKEN_VAZIO'

: "${RFA_ANDROID_KEYSTORE_B64:?RAFAELIA_ANDROID_KEYSTORE_B64_TOKEN_VAZIO}"
: "${RFA_ANDROID_STORE_PASSWORD:?RAFAELIA_ANDROID_STORE_PASSWORD_TOKEN_VAZIO}"
: "${RFA_ANDROID_KEY_ALIAS:?RAFAELIA_ANDROID_KEY_ALIAS_TOKEN_VAZIO}"
: "${RFA_ANDROID_KEY_PASSWORD:?RAFAELIA_ANDROID_KEY_PASSWORD_TOKEN_VAZIO}"
: "${RFA_EXPECTED_CERT_SHA256:?RAFAELIA_EXPECTED_CERT_SHA256_TOKEN_VAZIO}"
: "${RFA_SIGNER_ID:?RAFAELIA_SIGNER_ID_TOKEN_VAZIO}"

command -v jarsigner >/dev/null 2>&1 || fail 'JARSIGNER_NOT_FOUND'
command -v keytool >/dev/null 2>&1 || fail 'KEYTOOL_NOT_FOUND'

tmp="${RUNNER_TEMP:-/tmp}/rafaelia-aab-sign-$$"
mkdir -p "$tmp"
trap 'rm -rf "$tmp"' EXIT
ks="$tmp/rafaelia-signing.keystore"

printf '%s' "$RFA_ANDROID_KEYSTORE_B64" | base64 --decode > "$ks"
[ -s "$ks" ] || fail 'KEYSTORE_DECODE_EMPTY'

observed="$(
  keytool -list -v \
    -keystore "$ks" \
    -storepass "$RFA_ANDROID_STORE_PASSWORD" \
    -alias "$RFA_ANDROID_KEY_ALIAS" 2>/dev/null \
  | awk -F': ' '/SHA256:/{print $2; exit}' \
  | tr -d ':[:space:]' \
  | tr 'A-F' 'a-f'
)"
expected="$(printf '%s' "$RFA_EXPECTED_CERT_SHA256" | tr -d ':[:space:]' | tr 'A-F' 'a-f')"

[ "${#observed}" -eq 64 ] || fail 'OBSERVED_CERT_SHA256_INVALID'
[ "${#expected}" -eq 64 ] || fail 'EXPECTED_CERT_SHA256_INVALID'
[ "$observed" = "$expected" ] || fail 'CERT_SHA256_MISMATCH'

jarsigner \
  -keystore "$ks" \
  -storepass "$RFA_ANDROID_STORE_PASSWORD" \
  -keypass "$RFA_ANDROID_KEY_PASSWORD" \
  -signedjar "$signed_aab" \
  "$unsigned_aab" \
  "$RFA_ANDROID_KEY_ALIAS" >/dev/null

jarsigner -verify -strict "$signed_aab" >/dev/null

aab_sha="$(sha256sum "$signed_aab" | awk '{print $1}')"
unsigned_sha="$(sha256sum "$unsigned_aab" | awk '{print $1}')"

cat > "$receipt" <<EOF
schema=rafaelia.signed-aab/v1
source_sha=${RFA_SOURCE_SHA:-UNAVAILABLE}
run_id=${RFA_CI_RUN_ID:-UNAVAILABLE}
run_number=${RFA_CI_RUN_NUMBER:-UNAVAILABLE}
version_code=${RFA_VERSION_CODE:-TOKEN_VAZIO}
version_name=${RFA_VERSION_NAME:-TOKEN_VAZIO}
signer_id=$RFA_SIGNER_ID
android_key_alias=$RFA_ANDROID_KEY_ALIAS
expected_certificate_sha256=$expected
observed_certificate_sha256=$observed
certificate_match=PASS
unsigned_aab_sha256=$unsigned_sha
signed_aab_sha256=$aab_sha
signature_tool=JDK17_jarsigner
play_upload=NOT_RUN
private_key_material=SECRET_NOT_EXPORTED
EOF

printf 'RFA_SIGNED_AAB_SHA256=%s\n' "$aab_sha"
printf 'RFA_AAB_SIGN_RESULT=PASS\n'
