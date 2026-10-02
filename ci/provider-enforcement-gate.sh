#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_PROVIDER_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

ref="${RFA_PROVIDER_REF:-TOKEN_VAZIO}"
protected="${RFA_PROVIDER_REF_PROTECTED:-TOKEN_VAZIO}"
source_sha="${RFA_SOURCE_SHA:-TOKEN_VAZIO}"
ref_sha="${RFA_PROVIDER_REF_SHA:-TOKEN_VAZIO}"

printf 'PROVIDER_REF=%s\n' "$ref"
printf 'PROVIDER_REF_PROTECTED=%s\n' "$protected"
printf 'SOURCE_SHA=%s\n' "$source_sha"
printf 'PROVIDER_REF_SHA=%s\n' "$ref_sha"

[[ "$source_sha" =~ ^[0-9a-f]{40}$ ]] || fail "SOURCE_SHA_INVALID=$source_sha"
[[ "$ref_sha" =~ ^[0-9a-f]{40}$ ]] || fail "PROVIDER_REF_SHA_INVALID=$ref_sha"

# Authorial signed releases are allowed only from the current protected main ref.
test "$ref" = 'refs/heads/main' || fail "SIGNED_RELEASE_REF=$ref expected=refs/heads/main"
case "$protected" in
  true|TRUE|1) ;;
  *) fail "MAIN_PROTECTION=$protected expected=true" ;;
esac

test "$source_sha" = "$ref_sha" || \
  fail "SIGNED_SOURCE_NOT_CURRENT_PROTECTED_REF source=$source_sha ref_sha=$ref_sha"

echo 'PROVIDER_ENFORCEMENT=PASS'
echo 'SIGNED_RELEASE_SOURCE=CURRENT_PROTECTED_MAIN'
