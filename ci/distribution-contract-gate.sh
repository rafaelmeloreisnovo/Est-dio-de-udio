#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_DISTRIBUTION_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

for script in \
  ci/rafaelia-version.sh \
  ci/rafaelia-sign-aab.sh \
  ci/distribution-release.sh \
  ci/publish-update-page.sh \
  ci/publish-play.sh \
  ci/provider-bootstrap.sh \
  ci/rafaelia-sign-apk.sh
 do
  bash -n "$script" || fail "BASH_SYNTAX=$script"
done

grep -Fq 'compileSdk 36' app/build.gradle || fail 'COMPILE_SDK_36_MISSING'
grep -Fq 'targetSdk 36' app/build.gradle || fail 'TARGET_SDK_36_MISSING'
grep -Fq "versionCode envIntOrDefault('RFA_VERSION_CODE', 1)" app/build.gradle || fail 'SOURCE_BOUND_VERSION_CODE_MISSING'
grep -Fq 'platforms;android-36' ci/rafaelia-toolchain.sh || fail 'ANDROID36_PLATFORM_MISSING'
grep -Fq 'build-tools;36.0.0' ci/rafaelia-toolchain.sh || fail 'BUILD_TOOLS_36_MISSING'
grep -Fq 'build-tools/36.0.0' ci/rafaelia-sign-apk.sh || fail 'APKSIGNER_36_MISSING'
grep -Fq "targetSdkVersion:'36'" ci/apk-installability-static.sh || fail 'INSTALLABILITY_TARGET36_MISSING'
grep -Fq ':app:bundleRelease' ci/distribution-release.sh || fail 'AAB_BUILD_MISSING'
grep -Fq 'store-internal' .github/workflows/START.yml || fail 'STORE_INTERNAL_ROUTE_MISSING'
grep -Fq 'store-production' .github/workflows/START.yml || fail 'STORE_PRODUCTION_ROUTE_MISSING'
grep -Fq "PLAY_PRODUCTION_APPROVED=false" ci/distribution-release.sh || fail 'PRODUCTION_FAIL_CLOSED_MISSING'
grep -Fq 'allow_deletions' ci/provider-bootstrap.sh || fail 'PROVIDER_DELETE_POLICY_MISSING'
grep -Fq '"allow_deletions": false' ci/provider-bootstrap.sh || fail 'PROVIDER_DELETE_FALSE_MISSING'
if grep -Eiq -- '(--method[[:space:]]+DELETE|-X[[:space:]]+DELETE|curl[^\n]*-X[[:space:]]*DELETE)' ci/provider-bootstrap.sh; then
  fail 'PROVIDER_DELETE_CALL_DETECTED'
fi

printf 'DISTRIBUTION_CONTRACT_GATE=PASS\n'
printf 'UPDATE_VERSIONING=SOURCE_BOUND\n'
printf 'ANDROID_TARGET_API=36\n'
printf 'AAB_PUBLISH_PATH=PRESENT\n'
printf 'PAGES_UPDATE_FEED=PRESENT\n'
printf 'PLAY_INTERNAL_TRACK=PRESENT\n'
printf 'PLAY_PRODUCTION=EXPLICIT_GATE\n'
printf 'PROVIDER_DELETE=ABSENT\n'
