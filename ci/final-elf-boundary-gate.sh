#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"

efail() {
  printf '[FINAL_ELF_BOUNDARY_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

test -s "$apk" || efail "APK_MISSING=$apk"
test -n "${ANDROID_SDK_ROOT:-}" || efail 'ANDROID_SDK_ROOT=TOKEN_VAZIO'
ndk="$ANDROID_SDK_ROOT/ndk/27.2.12479018"
readelf="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
nm="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-nm"
test -x "$readelf" || efail "LLVM_READELF_MISSING=$readelf"
test -x "$nm" || efail "LLVM_NM_MISSING=$nm"

tmp="${RUNNER_TEMP:-/tmp}/rafaelia-final-elf-$$"
rm -rf "$tmp"
mkdir -p "$tmp"
trap 'rm -rf "$tmp"' EXIT

allowed_needed='libc.so libm.so libdl.so'

for abi in armeabi-v7a arm64-v8a; do
  so="$tmp/librafaelia_audio.$abi.so"
  unzip -p "$apk" "lib/$abi/librafaelia_audio.so" > "$so" || \
    efail "APK_NATIVE_ENTRY_MISSING=lib/$abi/librafaelia_audio.so"
  test -s "$so" || efail "APK_NATIVE_ENTRY_EMPTY=$abi"

  needed="$($readelf -d "$so" | sed -n 's/.*Shared library: \[\([^]]*\)\].*/\1/p')"
  unexpected=''
  for lib in $needed; do
    case " $allowed_needed " in
      *" $lib "*) ;;
      *) unexpected="$unexpected $lib" ;;
    esac
  done
  test -z "$unexpected" || efail "UNEXPECTED_NEEDED[$abi]=$unexpected"

  undefined_count="$($nm -u "$so" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"
  needed_count="$(printf '%s\n' "$needed" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' ')"

  printf 'FINAL_ELF_ABI=%s\n' "$abi"
  printf 'FINAL_ELF_NEEDED[%s]=%s\n' "$abi" "${needed//$'\n'/,}"
  printf 'FINAL_ELF_NEEDED_COUNT[%s]=%s\n' "$abi" "$needed_count"
  printf 'FINAL_ELF_DYNAMIC_UNDEFINED_COUNT[%s]=%s\n' "$abi" "$undefined_count"
  if test "$needed_count" -gt 0 || test "$undefined_count" -gt 0; then
    printf 'FINAL_ELF_CLASS[%s]=ANDROID_PLATFORM_LINKED\n' "$abi"
    printf 'FINAL_ELF_TRUE_FREESTANDING[%s]=NO\n' "$abi"
  else
    printf 'FINAL_ELF_CLASS[%s]=NO_DYNAMIC_PLATFORM_IMPORTS_OBSERVED\n' "$abi"
    printf 'FINAL_ELF_TRUE_FREESTANDING[%s]=NOT_CLAIMED\n' "$abi"
  fi
done

printf 'CORE_OBJECT_ZERO_UNDEFINED=SCOPE_SEPARATE_PRE_ANDROID_LINK\n'
printf 'FINAL_ELF_ALLOWED_NEEDED=%s\n' "${allowed_needed// /,}"
printf 'FINAL_ELF_BOUNDARY_GATE=PASS_EXECUTED_SCOPE\n'
printf 'CLAIM_ALLOWED=false\n'
