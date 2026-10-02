#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_PARSER_FUZZ][FAIL] %s\n' "$*" >&2
  exit 1
}

command -v clang >/dev/null 2>&1 || fail 'clang=TOKEN_VAZIO'

rm -f parser_fuzz_asan
clang \
  -std=c11 -O1 -g \
  -Wall -Wextra -Werror \
  -fsanitize=address,undefined \
  -fno-omit-frame-pointer \
  -Iapp/src/main/cpp \
  native/tests/parser_fuzz.c \
  app/src/main/cpp/rfa_container_core.c \
  app/src/main/cpp/rfa_rac1_core.c \
  -o parser_fuzz_asan

ASAN_OPTIONS='detect_leaks=1:halt_on_error=1:strict_string_checks=1' \
UBSAN_OPTIONS='halt_on_error=1:print_stacktrace=1' \
  ./parser_fuzz_asan

echo 'PARSER_FUZZ_CORPUS=8192'
echo 'PARSER_ASAN=PASS'
echo 'PARSER_UBSAN=PASS'
echo 'ZRF_CFR_HEADER_MUTATION=PASS_EXECUTED_SCOPE'
echo 'RAC1_BOUNDS_MUTATION=PASS_EXECUTED_SCOPE'
