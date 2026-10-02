#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_VERSION][FAIL] %s\n' "$*" >&2
  exit 1
}

source_sha="${RFA_SOURCE_SHA:-$(git rev-parse HEAD 2>/dev/null || true)}"
[[ "$source_sha" =~ ^[0-9a-f]{40}$ ]] || fail "SOURCE_SHA_INVALID=$source_sha"

git cat-file -e "$source_sha^{commit}" 2>/dev/null || fail "SOURCE_COMMIT_NOT_PRESENT=$source_sha"

# Deterministic, source-bound and monotonic for normal forward-moving commits.
# Seconds since 2020-01-01 keeps versionCode well below the Play maximum for decades.
base_epoch=1577836800
source_epoch="$(git show -s --format=%ct "$source_sha")"
[[ "$source_epoch" =~ ^[0-9]+$ ]] || fail "SOURCE_EPOCH_INVALID=$source_epoch"

version_code=$((source_epoch - base_epoch))
test "$version_code" -gt 0 || fail "VERSION_CODE_NONPOSITIVE=$version_code"
test "$version_code" -le 2100000000 || fail "VERSION_CODE_EXCEEDS_PLAY_MAX=$version_code"

short_sha="${source_sha:0:8}"
version_name="r${version_code}-${short_sha}"

if test -n "${GITHUB_ENV:-}"; then
  printf 'RFA_VERSION_CODE=%s\n' "$version_code" >> "$GITHUB_ENV"
  printf 'RFA_VERSION_NAME=%s\n' "$version_name" >> "$GITHUB_ENV"
fi

printf 'RFA_VERSION_CODE=%s\n' "$version_code"
printf 'RFA_VERSION_NAME=%s\n' "$version_name"
printf 'RFA_VERSION_SOURCE_SHA=%s\n' "$source_sha"
printf 'RFA_VERSION_POLICY=SOURCE_COMMIT_EPOCH_MINUS_2020_EPOCH\n'
printf 'RFA_VERSION_DETERMINISM=PASS_SOURCE_BOUND\n'
