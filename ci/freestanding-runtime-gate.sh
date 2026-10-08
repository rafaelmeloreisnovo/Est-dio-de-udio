#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_RUNTIME_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

test -n "${ANDROID_SDK_ROOT:-}" || fail 'ANDROID_SDK_ROOT=TOKEN_VAZIO'
ndk="$ANDROID_SDK_ROOT/ndk/27.2.12479018"
cc="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
nm="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-nm"
test -x "$cc" || fail "CLANG_MISSING=$cc"
test -x "$nm" || fail "LLVM_NM_MISSING=$nm"

# Freestanding micromodules are intentionally stricter than the Android/JNI edge.
# -Wshadow keeps local naming from silently masking another state coordinate.
# The object-symbol gate below forbids persistent writable named state entirely.
flags='-std=c11 -O3 -ffreestanding -fno-builtin -nostdinc -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -fno-common -fvisibility=hidden -ffunction-sections -fdata-sections -Wall -Wextra -Wshadow -Werror -Iapp/src/main/cpp'

sources=(
  dsp_core meter_core rfa_wave_core rfa_matrix_core rfa_block_core
  rfa_container_core rfa_fir_core rfa_time_core rfa_lms_core
  rfa_biquad_core rfa_voice_core rfa_measure_core rfa_rac1_core
  rfa_audio_format_core rfa_verbo_core
)

rm -rf .runtime-gate
mkdir -p .runtime-gate

for target in armv7a-linux-androideabi29 aarch64-linux-android29; do
  for src in "${sources[@]}"; do
    obj=".runtime-gate/${src}.${target}.o"
    undef=".runtime-gate/${src}.${target}.undefined"
    writable=".runtime-gate/${src}.${target}.writable-state"

    "$cc" --target="$target" $flags -c "app/src/main/cpp/$src.c" -o "$obj"

    "$nm" -u "$obj" > "$undef"
    if test -s "$undef"; then
      cat "$undef" >&2
      fail "UNDEFINED_RUNTIME_SYMBOL=$src target=$target"
    fi

    # ELF/NM symbol types that represent writable/common persistent storage:
    # B/b=.bss, C/c=common, D/d=.data, G/g=small data, S/s=small bss.
    # Read-only constants (R/r) remain allowed; automatic locals do not become
    # persistent global state and are not rejected by this object-level gate.
    "$nm" -a "$obj" | awk 'NF >= 3 && $2 ~ /^[bBcCdDgGsS]$/ { print }' > "$writable"
    if test -s "$writable"; then
      cat "$writable" >&2
      fail "WRITABLE_PERSISTENT_SYMBOL=$src target=$target"
    fi
  done
done

# Explicit regression sentinel for the historical ARM division-helper class.
if grep -RInE '__aeabi_(i|u)div|__aeabi_(l|ul)div|__(u?div|u?mod)di3' .runtime-gate; then
  fail 'ARM_DIVISION_RUNTIME_HELPER=DETECTED'
fi

echo 'ARMV7_RUNTIME_UNDEFINED=0'
echo 'AARCH64_RUNTIME_UNDEFINED=0'
echo 'ARMV7_WRITABLE_PERSISTENT_SYMBOLS=0'
echo 'AARCH64_WRITABLE_PERSISTENT_SYMBOLS=0'
echo 'LOCAL_SHADOW_DIAGNOSTIC=-Wshadow,-Werror'
echo 'READ_ONLY_NAMED_CONSTANTS=ALLOWED'
echo 'AUTOMATIC_LOCAL_STATE=STACK_OR_REGISTER_SCOPE'
echo 'ARM_DIVISION_RUNTIME_HELPER=ABSENT'
echo 'FREESTANDING_RUNTIME_GATE=PASS'
