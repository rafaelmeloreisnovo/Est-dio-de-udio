#!/usr/bin/env bash
set -euo pipefail

# No SDK/NDK downloads, Gradle daemon, APK publish, artifacts or external actions.
# This is an honest source/host preflight, not a device or full build receipt.
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

fail() { printf '[RAFAELIA_FAST_GATE][FAIL] %s\n' "$*" >&2; exit 1; }
[[ "${RFA_SOURCE_SHA:-}" =~ ^[0-9a-f]{40}$ ]] || fail 'EXACT_SHA_TOKEN_VAZIO'
test "$(git rev-parse HEAD)" = "$RFA_SOURCE_SHA" || fail 'EXACT_SHA_MISMATCH'

printf 'FAST_SOURCE_SHA=%s\n' "$RFA_SOURCE_SHA"
printf 'FAST_SCOPE=SOURCE_HOST_ONLY\n'
printf 'FAST_FULL_APK_BUILD=NOT_RUN\n'
printf 'FAST_ARM_DEVICE=NOT_RUN\n'

# Fail promptly before any heavyweight toolchain work.
while IFS= read -r -d '' script; do
    bash -n "$script" || fail "BASH_SYNTAX:$script"
done < <(find ci -type f -name '*.sh' -print0)
bash ci/rafaelia-pipeline.sh topology
bash ci/distribution-contract-gate.sh
bash ci/test-provider-preflight.sh
bash ci/audio-session-gate.sh
bash ci/zipraf-lowlevel-gate.sh

# Run freestanding C host vectors with the same strict flags as the full quality
# stage. ARMv7 and AArch64 ELF gates remain mandatory in full mode.
command -v clang >/dev/null 2>&1 || fail 'CLANG_HOST_MISSING'
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
cflags=(-std=c11 -O2 -ffreestanding -fno-builtin -nostdinc
        -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables
        -fno-common -Wall -Wextra -Werror -Iapp/src/main/cpp)
clang "${cflags[@]}" native/tests/dsp_smoke.c app/src/main/cpp/dsp_core.c -o "$tmp/dsp"
"$tmp/dsp"
clang "${cflags[@]}" native/tests/meter_smoke.c app/src/main/cpp/meter_core.c -o "$tmp/meter"
"$tmp/meter"
clang "${cflags[@]}" native/tests/manifold_smoke.c \
  app/src/main/cpp/rfa_wave_core.c \
  app/src/main/cpp/rfa_matrix_core.c \
  app/src/main/cpp/rfa_block_core.c \
  app/src/main/cpp/rfa_container_core.c \
  app/src/main/cpp/rfa_fir_core.c \
  app/src/main/cpp/rfa_time_core.c \
  app/src/main/cpp/rfa_lms_core.c \
  app/src/main/cpp/rfa_biquad_core.c \
  app/src/main/cpp/rfa_voice_core.c \
  app/src/main/cpp/rfa_measure_core.c \
  app/src/main/cpp/rfa_rac1_core.c \
  app/src/main/cpp/rfa_audio_format_core.c \
  -o "$tmp/manifold"
"$tmp/manifold"
clang "${cflags[@]}" native/tests/verbo_smoke.c app/src/main/cpp/rfa_verbo_core.c -o "$tmp/verbo"
"$tmp/verbo"

printf 'FAST_JAVA_NO_THIRD_PARTY=PASS_SOURCE_SCOPE\n'
printf 'FAST_C_HOST_VECTORS=PASS\n'
printf 'FAST_APK_AND_ABI_CROSS_BUILD=NOT_RUN\n'
printf 'RAFAELIA_FAST_GATE=PASS_SOURCE_HOST_ONLY\n'
