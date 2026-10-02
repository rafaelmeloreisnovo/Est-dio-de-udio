#!/usr/bin/env bash
set -euo pipefail

phase="${1:-}"

log() {
  printf '[RAFAELIA_PIPELINE] %s\n' "$*"
}

fail() {
  printf '[RAFAELIA_PIPELINE][FAIL] %s\n' "$*" >&2
  exit 1
}

require_env() {
  local name="$1"
  test -n "${!name:-}" || fail "$name=TOKEN_VAZIO"
}

bootstrap() {
  log 'PHASE=10_CAPACITY_BOOTSTRAP'
  bash ci/rafaelia-toolchain.sh java
  bash ci/rafaelia-toolchain.sh android
  bash ci/rafaelia-toolchain.sh gradle
  bash ci/rafaelia-toolchain.sh report
}

topology() {
  log 'PHASE=20_TOPOLOGY_BOUNDARY'

  mapfile -t active < <(
    find .github/workflows -maxdepth 1 -type f \( -name '*.yml' -o -name '*.yaml' \) \
      -print | LC_ALL=C sort
  )

  if test "${#active[@]}" -ne 1; then
    printf 'ACTIVE_WORKFLOW=%s\n' "${active[@]:-TOKEN_VAZIO}"
    fail "ACTIVE_WORKFLOW_COUNT=${#active[@]} expected=1"
  fi
  test "${active[0]}" = ".github/workflows/START.yml" || \
    fail "CANONICAL_WORKFLOW=${active[0]} expected=.github/workflows/START.yml"

  mapfile -t external_uses < <(
    grep -RhoE 'uses:[[:space:]]+[^[:space:]]+' .github/workflows \
      | awk '{print $2}' \
      | grep -v '^\./' \
      | sort -u || true
  )

  if test "${#external_uses[@]}" -ne 0; then
    printf 'UNAPPROVED_EXTERNAL_ACTION=%s\n' "${external_uses[@]}"
    fail 'external GitHub Action dependency detected'
  fi

  echo 'ACTIVE_WORKFLOW_COUNT=1'
  echo 'CANONICAL_WORKFLOW=.github/workflows/START.yml'
  echo 'NODE_APP_RUNTIME=0'
  echo 'EXTERNAL_JS_ACTIONS=NONE'
}

quality() {
  log 'PHASE=30_QUALITY_CAPABILITY'

  gradle :app:rafaeliaBuildContract --no-daemon --stacktrace
  gradle :app:rafaeliaJavaGate --no-daemon --stacktrace

  local core_cflags
  core_cflags='-std=c11 -O2 -ffreestanding -fno-builtin -nostdinc -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -fno-common -Wall -Wextra -Werror -Iapp/src/main/cpp'

  clang $core_cflags native/tests/dsp_smoke.c app/src/main/cpp/dsp_core.c -o dsp_smoke
  ./dsp_smoke

  clang $core_cflags native/tests/meter_smoke.c app/src/main/cpp/meter_core.c -o meter_smoke
  ./meter_smoke

  clang $core_cflags native/tests/manifold_smoke.c \
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
    -o manifold_smoke
  ./manifold_smoke

  local core_c core_all
  core_c='app/src/main/cpp/dsp_core.c app/src/main/cpp/meter_core.c app/src/main/cpp/rfa_wave_core.c app/src/main/cpp/rfa_matrix_core.c app/src/main/cpp/rfa_block_core.c app/src/main/cpp/rfa_container_core.c app/src/main/cpp/rfa_fir_core.c app/src/main/cpp/rfa_time_core.c app/src/main/cpp/rfa_lms_core.c app/src/main/cpp/rfa_biquad_core.c app/src/main/cpp/rfa_voice_core.c app/src/main/cpp/rfa_measure_core.c app/src/main/cpp/rfa_rac1_core.c'
  core_all="$core_c app/src/main/cpp/dsp_core.h app/src/main/cpp/meter_core.h app/src/main/cpp/rfa_wave_core.h app/src/main/cpp/rfa_matrix_core.h app/src/main/cpp/rfa_block_core.h app/src/main/cpp/rfa_container_core.h app/src/main/cpp/rfa_fir_core.h app/src/main/cpp/rfa_time_core.h app/src/main/cpp/rfa_lms_core.h app/src/main/cpp/rfa_biquad_core.h app/src/main/cpp/rfa_voice_core.h app/src/main/cpp/rfa_measure_core.h app/src/main/cpp/rfa_rac1_core.h app/src/main/cpp/rfa_sine_q15.h app/src/main/cpp/rfa_core_types.h"

  if grep -nRE '^[[:space:]]*#include[[:space:]]*<' $core_all; then
    fail 'system header reached the freestanding core'
  fi

  if grep -nRE '\b(malloc|calloc|realloc|free|memcpy|memmove|memset|memcmp|printf|fprintf|snprintf|puts|open|close|read|write|sqrt|pow|log|exp|sin|cos|tan)[[:space:]]*\(' $core_c; then
    fail 'forbidden hosted/runtime dependency in freestanding core'
  fi

  if grep -nP '^\s*static\s+(?!const\b)[^({]*;' $core_c; then
    fail 'mutable file-scope state is forbidden in freestanding core'
  fi

  if grep -RInE '^[[:space:]]*import[[:space:]]+(androidx|kotlin|com\.google)\.' app/src/main/java; then
    fail 'external Java/UI framework dependency detected'
  fi

  if grep -nE '^[[:space:]]*(implementation|api|compileOnly|runtimeOnly|kapt|ksp)[[:space:]]' app/build.gradle; then
    fail 'Gradle runtime/library dependency declaration detected'
  fi

  if grep -nE 'minifyEnabled[[:space:]]+true|shrinkResources[[:space:]]+true' app/build.gradle; then
    fail 'R8/resource shrink path must remain disabled'
  fi

  test -f gradle/rafaelia-authorial.gradle
  grep -Fq 'apply from: rootProject.file("gradle/rafaelia-authorial.gradle")' app/build.gradle
  grep -Fq 'tasks.register("rafaeliaJavaGate")' gradle/rafaelia-authorial.gradle
  grep -Fq 'tasks.register("rafaeliaDebugApk")' gradle/rafaelia-authorial.gradle

  local jni_files jni_count
  jni_files="$(grep -RIl 'JNIEXPORT' app/src/main/cpp || true)"
  jni_count="$(printf '%s\n' "$jni_files" | sed '/^[[:space:]]*$/d' | wc -l)"
  test "$jni_count" -eq 1 || fail "JNI_IMPLEMENTATION_FILES=$jni_count expected=1"
  test "$jni_files" = 'app/src/main/cpp/jni_bridge.c' || \
    fail "JNI_CANONICAL_EDGE=$jni_files"

  echo 'JAVA_THIRD_PARTY_DEPS=0'
  echo 'ANDROIDX=0'
  echo 'KOTLIN=0'
  echo 'R8_SHRINK=0'
  echo 'AUTHORIAL_GRADLE=V1'
  echo 'JNI_IMPLEMENTATION_FILES=1'
  echo 'QUALITY_CAPABILITY=PASS_EXECUTED_SCOPE'
}

architecture() {
  log 'PHASE=40_ARCHITECTURE_ABI'

  local ndk cc nm flags src
  ndk="$ANDROID_SDK_ROOT/ndk/27.2.12479018"
  cc="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
  nm="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-nm"
  flags='-std=c11 -O3 -ffreestanding -fno-builtin -nostdinc -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -fno-common -fvisibility=hidden -ffunction-sections -fdata-sections -Wall -Wextra -Werror -Iapp/src/main/cpp'

  for src in dsp_core meter_core rfa_wave_core rfa_matrix_core rfa_block_core rfa_container_core rfa_fir_core rfa_time_core rfa_lms_core rfa_biquad_core rfa_voice_core rfa_measure_core rfa_rac1_core; do
    "$cc" --target=armv7a-linux-androideabi29 $flags -c "app/src/main/cpp/$src.c" -o "$src.armv7.o"
    "$nm" -u "$src.armv7.o" > "$src.armv7.undefined"
    test ! -s "$src.armv7.undefined" || { cat "$src.armv7.undefined"; fail "ARMV7_UNDEFINED=$src"; }
  done

  : > abi.actual
  for src in dsp_core meter_core rfa_wave_core rfa_matrix_core rfa_block_core rfa_container_core rfa_fir_core rfa_time_core rfa_lms_core rfa_biquad_core rfa_voice_core rfa_measure_core rfa_rac1_core; do
    "$nm" --defined-only --extern-only "$src.armv7.o" | awk -v src="$src" '{print src ":" $NF}' >> abi.actual
  done
  sort -o abi.actual abi.actual
  grep -v '^[[:space:]]*#' native/abi/public_symbols_v1.txt | grep -v '^[[:space:]]*$' | sort > abi.expected
  diff -u abi.expected abi.actual

  for src in dsp_core meter_core rfa_wave_core rfa_matrix_core rfa_block_core rfa_container_core rfa_fir_core rfa_time_core rfa_lms_core rfa_biquad_core rfa_voice_core rfa_measure_core rfa_rac1_core; do
    "$cc" --target=aarch64-linux-android29 $flags -c "app/src/main/cpp/$src.c" -o "$src.aarch64.o"
    "$nm" -u "$src.aarch64.o" > "$src.aarch64.undefined"
    test ! -s "$src.aarch64.undefined" || { cat "$src.aarch64.undefined"; fail "AARCH64_UNDEFINED=$src"; }
  done

  echo 'ARMV7_ZERO_UNDEFINED=PASS'
  echo 'ARMV7_ABI_MANIFEST=PASS'
  echo 'AARCH64_ZERO_UNDEFINED=PASS'
}

evidence() {
  log 'PHASE=50_EVIDENCE_PREPARE'
  require_env RFA_SOURCE_SHA
  require_env RFA_CI_RUN_ID
  require_env RFA_CI_RUN_NUMBER

  rm -rf /tmp/rfa_sha_test
  mkdir -p /tmp/rfa_sha_test
  javac -d /tmp/rfa_sha_test \
    app/src/main/java/io/rafaelia/audiostudio/LowSha256.java \
    native/tests/LowSha256Smoke.java
  java -cp /tmp/rfa_sha_test io.rafaelia.audiostudio.LowSha256Smoke

  bash ci/rafaelia-origin-assets.sh
  mkdir -p app/src/main/assets
  cat > app/src/main/assets/ci_provenance_v1.txt <<EOF
schema=rafaelia.ci-provenance/v1
pipeline=RAFAELIA_AUDIO_START_V1
quality_method=CAPABILITY_QUALITY_V1
source_sha=$RFA_SOURCE_SHA
run_id=$RFA_CI_RUN_ID
run_number=$RFA_CI_RUN_NUMBER
gradle_authorial_contract=PASS
java_compile_gate=PASS
assurance_model_smoke=PASS
host_dsp_smoke=PASS
host_meter_smoke=PASS
host_manifold_smoke=PASS
freestanding_source_gate=PASS
low_dependency_gate=PASS
authorial_toolchain_bootstrap=PASS
node_app_runtime=0
external_js_actions=NONE
armv7_zero_undefined=PASS
armv7_abi_manifest=PASS
aarch64_zero_undefined=PASS
low_java_sha256_kat=PASS
assemble_debug=PENDING_AT_EMBED
external_standard_audit=NOT_AUDITED
claim_allowed=false
EOF
  echo 'EVIDENCE_PREPARE=PASS_EXECUTED_SCOPE'
}

build_debug() {
  log 'PHASE=60_BUILD_DEBUG'
  gradle :app:rafaeliaDebugApk --no-daemon --stacktrace
  test -f app/build/outputs/apk/debug/app-debug.apk || fail 'DEBUG_APK=TOKEN_VAZIO'
  echo 'DEBUG_APK_BUILD=PASS'
}

binary_receipt() {
  log 'PHASE=70_BINARY_RECEIPT'
  require_env RFA_SOURCE_SHA
  require_env RFA_CI_RUN_ID

  local apk apk_sha entry_manifest entry_manifest_sha entry_count receipt entry digest
  apk='app/build/outputs/apk/debug/app-debug.apk'
  test -f "$apk" || fail 'DEBUG_APK=TOKEN_VAZIO'
  apk_sha="$(sha256sum "$apk" | awk '{print $1}')"
  entry_manifest="$RUNNER_TEMP/apk-entry-sha256.txt"
  receipt="$RUNNER_TEMP/binary_origin_receipt_v1.txt"
  : > "$entry_manifest"

  while IFS= read -r entry; do
    case "$entry" in
      */) continue ;;
    esac
    digest="$(unzip -p "$apk" "$entry" | sha256sum | awk '{print $1}')"
    printf '%s  %s\n' "$digest" "$entry" >> "$entry_manifest"
  done < <(unzip -Z1 "$apk" | LC_ALL=C sort)

  entry_manifest_sha="$(sha256sum "$entry_manifest" | awk '{print $1}')"
  entry_count="$(wc -l < "$entry_manifest" | tr -d ' ')"

  cat > "$receipt" <<EOF
schema=rafaelia.binary-origin/v1
pipeline=RAFAELIA_AUDIO_START_V1
source_sha=$RFA_SOURCE_SHA
run_id=$RFA_CI_RUN_ID
apk_sha256=$apk_sha
apk_entry_manifest_sha256=$entry_manifest_sha
apk_entry_count=$entry_count
classes.dex=GENERATED_DEX_MIXED_PROJECT_BYTECODE_PLATFORM_REFERENCES
lib/*/librafaelia_audio.so=PROJECT_NATIVE_BINARY_WITH_ANDROID_ABI_EDGE
AndroidManifest.xml=GENERATED_FROM_PROJECT_MANIFEST
resources.arsc=GENERATED_ANDROID_RESOURCE_TABLE
META-INF/*=BUILD_OR_SIGNATURE_METADATA
claim=BYTE_ORIGIN_CLASSIFICATION_NOT_THIRD_PARTY_AUTHORSHIP_ERASURE
external_standard_audit=NOT_AUDITED
claim_allowed=false
EOF

  echo "RFA_APK_SHA256=$apk_sha" >> "$GITHUB_ENV"
  echo "RFA_BINARY_RECEIPT=$receipt" >> "$GITHUB_ENV"
  echo "RFA_ENTRY_MANIFEST=$entry_manifest" >> "$GITHUB_ENV"
  echo 'BINARY_RECEIPT=PASS'
}

publish_live() {
  log 'PHASE=90_DELIVERY_LIVE_DEBUG'
  require_env RFA_SOURCE_SHA
  require_env RFA_CI_RUN_ID
  require_env RFA_CI_RUN_NUMBER
  require_env RFA_APK_SHA256
  require_env RFA_BINARY_RECEIPT
  require_env RFA_ENTRY_MANIFEST
  command -v gh >/dev/null 2>&1 || fail 'GH_CLI_UNAVAILABLE'

  local short_sha tag apk_name release_receipt
  short_sha="$(printf '%s' "$RFA_SOURCE_SHA" | cut -c1-12)"
  tag="rafaelia-live-$RFA_CI_RUN_ID"
  apk_name="rafaelia-audio-studio-$short_sha.apk"
  release_receipt="$RUNNER_TEMP/live_release_receipt_v1.txt"
  cp app/build/outputs/apk/debug/app-debug.apk "$RUNNER_TEMP/$apk_name"

  cat > "$release_receipt" <<EOF
schema=rafaelia.live-release/v1
pipeline=RAFAELIA_AUDIO_START_V1
source_sha=$RFA_SOURCE_SHA
run_id=$RFA_CI_RUN_ID
run_number=$RFA_CI_RUN_NUMBER
apk_sha256=$RFA_APK_SHA256
signing_mode=DEBUG_NONAUTHORIAL
external_js_actions=NONE
external_standard_audit=NOT_AUDITED
claim_allowed=false
EOF

  gh release create "$tag" \
    "$RUNNER_TEMP/$apk_name#Rafaelia Audio Studio debug APK" \
    "$RFA_BINARY_RECEIPT#Binary origin receipt" \
    "$RFA_ENTRY_MANIFEST#APK entry SHA-256 manifest" \
    "$release_receipt#Live release receipt" \
    --repo "$GITHUB_REPOSITORY" \
    --target "$RFA_SOURCE_SHA" \
    --prerelease \
    --title "Rafaelia Live $short_sha" \
    --notes-file "$release_receipt"

  echo 'LIVE_DEBUG_DELIVERY=PASS'
}

signed_release() {
  log 'PHASE=90_DELIVERY_SIGNED'
  require_env RFA_SOURCE_SHA
  require_env RFA_CI_RUN_ID
  require_env RFA_CI_RUN_NUMBER
  require_env RFA_SIGNER_ID
  require_env RFA_EXPECTED_CERT_SHA256
  require_env RFA_ANDROID_KEY_ALIAS
  require_env RFA_ANDROID_KEYSTORE_B64
  require_env RFA_ANDROID_STORE_PASSWORD
  require_env RFA_ANDROID_KEY_PASSWORD

  bash ci/rafaelia-origin-assets.sh
  mkdir -p app/src/main/assets
  cat > app/src/main/assets/ci_provenance_v1.txt <<EOF
schema=rafaelia.ci-provenance/v1
pipeline=RAFAELIA_AUDIO_START_V1
quality_method=CAPABILITY_QUALITY_V1
source_sha=$RFA_SOURCE_SHA
run_id=$RFA_CI_RUN_ID
run_number=$RFA_CI_RUN_NUMBER
upstream_gate=PASS_EXECUTED_SCOPE
authorial_toolchain_bootstrap=PASS
node_app_runtime=0
external_js_actions=NONE
signing_mode=AUTHORIAL_ANDROID_APKSIGNER
signing_certificate_match=PENDING_AT_EMBED
signed_release_publish=PENDING_AT_EMBED
external_standard_audit=NOT_AUDITED
claim_allowed=false
EOF

  gradle :app:assembleRelease --no-daemon --stacktrace

  local unsigned signed receipt tag
  unsigned='app/build/outputs/apk/release/app-release-unsigned.apk'
  signed="$RUNNER_TEMP/rafaelia-audio-studio-signed.apk"
  receipt="$RUNNER_TEMP/rafaelia_signed_release_receipt_v1.txt"
  test -f "$unsigned" || fail 'UNSIGNED_RELEASE_APK=TOKEN_VAZIO'
  bash ci/rafaelia-sign-apk.sh "$unsigned" "$signed" "$receipt"

  tag="rafaelia-signed-$RFA_CI_RUN_ID"
  gh release create "$tag" \
    "$signed#Authorially signed Rafaelia Audio Studio APK" \
    "$receipt#Signing receipt" \
    --repo "$GITHUB_REPOSITORY" \
    --target "$RFA_SOURCE_SHA" \
    --prerelease \
    --title "Rafaelia Signed $RFA_CI_RUN_ID" \
    --notes-file "$receipt"

  bash ci/publish-signing-page.sh "$receipt"
  echo 'SIGNED_DELIVERY=PASS_EXECUTED_SCOPE'
}

summary() {
  log 'PHASE=99_RECEIPT_SUMMARY'
  if test -n "${GITHUB_STEP_SUMMARY:-}"; then
    cat >> "$GITHUB_STEP_SUMMARY" <<EOF
## Rafaelia Audio START V1

- source_sha: \`${RFA_SOURCE_SHA:-TOKEN_VAZIO}\`
- pipeline: \`RAFAELIA_AUDIO_START_V1\`
- quality_method: \`CAPABILITY_QUALITY_V1\`
- SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
- TOKEN_VAZIO != 0
- IMPLEMENTED_UNTESTED != PASS
- external_standard_audit: \`NOT_AUDITED\`
- claim_allowed: \`false\`
EOF
  fi
}

case "$phase" in
  bootstrap) bootstrap ;;
  topology) topology ;;
  quality) quality ;;
  architecture) architecture ;;
  evidence) evidence ;;
  build-debug) build_debug ;;
  binary-receipt) binary_receipt ;;
  publish-live) publish_live ;;
  signed-release) signed_release ;;
  summary) summary ;;
  *)
    printf 'usage: %s {bootstrap|topology|quality|architecture|evidence|build-debug|binary-receipt|publish-live|signed-release|summary}\n' "$0" >&2
    exit 64
    ;;
esac
