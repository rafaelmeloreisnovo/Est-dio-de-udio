#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

fail() {
  printf '[COMPILE_DEBUG_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

printf 'COMPILE_SCOPE=JAVA_DEBUG+NATIVE_DEBUG\n'
printf 'COMPILE_SOURCE_SHA=%s\n' "${RFA_SOURCE_SHA:-TOKEN_VAZIO}"

# Compile Android Java edge and C/CMake native edge as an explicit CI gate.
gradle \
  :app:compileDebugJavaWithJavac \
  :app:externalNativeBuildDebug \
  --no-daemon \
  --stacktrace

java_class="$(find app/build -type f -path '*/io/rafaelia/audiostudio/MainActivity.class' -print -quit 2>/dev/null || true)"
test -n "$java_class" || fail 'JAVA_CLASS=TOKEN_VAZIO'

mapfile -t native_libs < <(
  find app/build -type f -name 'librafaelia_audio.so' -print 2>/dev/null | LC_ALL=C sort -u
)

test "${#native_libs[@]}" -ge 2 || \
  fail "NATIVE_SHARED_OBJECT_COUNT=${#native_libs[@]} expected>=2"

armv7=0
arm64=0
for lib in "${native_libs[@]}"; do
  case "$lib" in
    *armeabi-v7a*) armv7=1 ;;
    *arm64-v8a*) arm64=1 ;;
  esac
done

test "$armv7" -eq 1 || fail 'NATIVE_ABI_ARMEABI_V7A=TOKEN_VAZIO'
test "$arm64" -eq 1 || fail 'NATIVE_ABI_ARM64_V8A=TOKEN_VAZIO'

printf 'JAVA_CLASS=%s\n' "$java_class"
printf 'NATIVE_SHARED_OBJECT_COUNT=%s\n' "${#native_libs[@]}"
printf 'NATIVE_ABI_ARMEABI_V7A=PASS\n'
printf 'NATIVE_ABI_ARM64_V8A=PASS\n'
printf 'COMPILE_DEBUG_GATE=PASS_EXECUTED_SCOPE\n'
printf 'EXTERNAL_STANDARD_AUDIT=NOT_AUDITED\n'
printf 'CLAIM_ALLOWED=false\n'
