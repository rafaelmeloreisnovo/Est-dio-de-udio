#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

cc="${CC:-clang}"
nm_bin="${NM:-nm}"
flags=(
  -std=c11 -O2 -ffreestanding -fno-builtin -nostdinc
  -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables
  -fno-common -fvisibility=hidden -ffunction-sections -fdata-sections
  -Wall -Wextra -Werror -Iapp/src/main/cpp
)

obj="${TMPDIR:-/tmp}/rfa_audio_format_core.$$.o"
smoke="${TMPDIR:-/tmp}/rfa_audio_format_core_smoke.$$"
undef="${TMPDIR:-/tmp}/rfa_audio_format_core_undefined.$$.txt"
trap 'rm -f "$obj" "$smoke" "$undef"' EXIT

"$cc" "${flags[@]}" -c app/src/main/cpp/rfa_audio_format_core.c -o "$obj"
"$nm_bin" -u "$obj" > "$undef"
test ! -s "$undef" || {
  cat "$undef"
  echo 'AUDIO_FORMAT_CORE_ZERO_UNDEFINED=FAIL' >&2
  exit 1
}

"$cc" -std=c11 -O2 -Wall -Wextra -Werror \
  -Iapp/src/main/cpp \
  native/tests/audio_format_core_smoke.c \
  app/src/main/cpp/rfa_audio_format_core.c \
  -o "$smoke"
"$smoke"

echo 'AUDIO_FORMAT_CORE_FREESTANDING_COMPILE=PASS'
echo 'AUDIO_FORMAT_CORE_ZERO_UNDEFINED=PASS'
echo 'AUDIO_FORMAT_CORE_SMOKE=PASS'
echo 'AUDIO_FORMAT_CORE_FORMATS=RAW_PCM16,WAV_PCM16,AIFF_PCM16,AU_PCM16,CAF_LPCM16'
echo 'AUDIO_FORMAT_CORE_LIBC=NONE'
echo 'AUDIO_FORMAT_CORE_HEAP=NONE'
