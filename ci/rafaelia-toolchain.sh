#!/usr/bin/env bash
set -euo pipefail

cmd="${1:-all}"

die() {
  printf 'RAFAELIA_TOOLCHAIN_FAIL=%s\n' "$1" >&2
  exit 1
}

setup_java() {
  local home="${JAVA_HOME_17_X64:-}"
  if [ -z "$home" ] || [ ! -x "$home/bin/java" ]; then
    if command -v java >/dev/null 2>&1; then
      local version
      version="$(java -version 2>&1 | head -n1 || true)"
      case "$version" in
        *"17."*) home="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")" ;;
        *) die "JAVA17_TOKEN_VAZIO" ;;
      esac
    else
      die "JAVA17_TOKEN_VAZIO"
    fi
  fi

  export JAVA_HOME="$home"
  export PATH="$JAVA_HOME/bin:$PATH"

  if [ -n "${GITHUB_ENV:-}" ]; then
    printf 'JAVA_HOME=%s\n' "$JAVA_HOME" >> "$GITHUB_ENV"
  fi
  if [ -n "${GITHUB_PATH:-}" ]; then
    printf '%s\n' "$JAVA_HOME/bin" >> "$GITHUB_PATH"
  fi

  java -version
  javac -version
  printf 'RAFAELIA_JAVA17=PASS\n'
}

setup_android() {
  local root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
  [ -n "$root" ] || die "ANDROID_SDK_ROOT_TOKEN_VAZIO"
  [ -d "$root" ] || die "ANDROID_SDK_ROOT_NOT_FOUND"

  local sdkmanager=""
  for candidate in     "$root/cmdline-tools/latest/bin/sdkmanager"     "$root/cmdline-tools/bin/sdkmanager"     "$root/tools/bin/sdkmanager"
  do
    if [ -x "$candidate" ]; then
      sdkmanager="$candidate"
      break
    fi
  done

  if [ -z "$sdkmanager" ] && command -v sdkmanager >/dev/null 2>&1; then
    sdkmanager="$(command -v sdkmanager)"
  fi

  [ -n "$sdkmanager" ] || die "SDKMANAGER_TOKEN_VAZIO"

  export ANDROID_SDK_ROOT="$root"
  export ANDROID_HOME="$root"
  export PATH="$(dirname "$sdkmanager"):$ANDROID_SDK_ROOT/platform-tools:$PATH"

  if [ -n "${GITHUB_ENV:-}" ]; then
    printf 'ANDROID_SDK_ROOT=%s\n' "$ANDROID_SDK_ROOT" >> "$GITHUB_ENV"
    printf 'ANDROID_HOME=%s\n' "$ANDROID_HOME" >> "$GITHUB_ENV"
  fi
  if [ -n "${GITHUB_PATH:-}" ]; then
    printf '%s\n' "$(dirname "$sdkmanager")" >> "$GITHUB_PATH"
    printf '%s\n' "$ANDROID_SDK_ROOT/platform-tools" >> "$GITHUB_PATH"
  fi

  yes | "$sdkmanager" --licenses >/dev/null || true
  "$sdkmanager"     "platform-tools"     "platforms;android-35"     "build-tools;35.0.0"     "ndk;27.2.12479018"     "cmake;3.22.1"

  [ -d "$ANDROID_SDK_ROOT/ndk/27.2.12479018" ] || die "NDK_INSTALL_FAIL"
  printf 'RAFAELIA_ANDROID_SDK=PASS\n'
}

setup_gradle() {
  local version="8.11.1"
  local sha256="f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6"
  local base="${RUNNER_TEMP:-/tmp}/rafaelia-gradle"
  local zip="$base/gradle-$version-bin.zip"
  local home="$base/gradle-$version"

  mkdir -p "$base"

  if [ ! -x "$home/bin/gradle" ]; then
    curl --fail --location --silent --show-error       "https://services.gradle.org/distributions/gradle-$version-bin.zip"       --output "$zip"
    printf '%s  %s\n' "$sha256" "$zip" | sha256sum --check --strict
    rm -rf "$home"
    unzip -q "$zip" -d "$base"
  fi

  [ -x "$home/bin/gradle" ] || die "GRADLE_INSTALL_FAIL"

  export PATH="$home/bin:$PATH"
  if [ -n "${GITHUB_PATH:-}" ]; then
    printf '%s\n' "$home/bin" >> "$GITHUB_PATH"
  fi

  gradle --version
  printf 'RAFAELIA_GRADLE=PASS\n'
  printf 'RAFAELIA_GRADLE_SHA256=%s\n' "$sha256"
}

report() {
  printf 'RAFAELIA_TOOLCHAIN_CONTRACT=V1\n'
  printf 'NODE_APP_RUNTIME=0\n'
  printf 'NODE_JS_ACTIONS_ALLOWED=NONE\n'
  printf 'CHECKOUT=AUTHORIAL_GIT_SHELL\n'
  printf 'JAVA17=RUNNER_IMAGE_VERIFIED\n'
  printf 'ANDROID_SDK=RUNNER_IMAGE_PLUS_PINNED_PACKAGES\n'
  printf 'GRADLE=PINNED_BINARY_SHA256\n'
  printf 'GH_CLI=RUNNER_PLATFORM_EDGE\n'
}

case "$cmd" in
  java) setup_java ;;
  android) setup_android ;;
  gradle) setup_gradle ;;
  report) report ;;
  all)
    setup_java
    setup_android
    setup_gradle
    report
    ;;
  *) die "UNKNOWN_COMMAND_$cmd" ;;
esac
