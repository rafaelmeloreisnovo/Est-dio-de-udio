#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_DISTRIBUTION][FAIL] %s\n' "$*" >&2
  exit 1
}

: "${RFA_SOURCE_SHA:?SOURCE_SHA_TOKEN_VAZIO}"
: "${RFA_CI_RUN_ID:?CI_RUN_ID_TOKEN_VAZIO}"
: "${RFA_CI_RUN_NUMBER:?CI_RUN_NUMBER_TOKEN_VAZIO}"
: "${RFA_VERSION_CODE:?VERSION_CODE_TOKEN_VAZIO}"
: "${RFA_VERSION_NAME:?VERSION_NAME_TOKEN_VAZIO}"

mode="${RFA_DELIVERY_MODE:-signed-release}"
case "$mode" in
  signed-release|store-internal|store-production) ;;
  *) fail "DELIVERY_MODE_INVALID=$mode" ;;
esac

# Store modes fail before creating a GitHub release if the Google authority is absent.
case "$mode" in
  store-internal|store-production)
    [ -n "${RFA_PLAY_SERVICE_ACCOUNT_B64:-}" ] || fail 'PLAY_SERVICE_ACCOUNT_B64=TOKEN_VAZIO'
    ;;
esac
if [ "$mode" = store-production ] && [ "${RFA_PLAY_PRODUCTION_APPROVED:-false}" != true ]; then
  fail 'PLAY_PRODUCTION_APPROVED=false'
fi

# Existing signed-release path remains the canonical APK signing and release edge.
bash ci/rafaelia-pipeline.sh signed-release

gradle :app:bundleRelease --no-daemon --stacktrace
unsigned_aab='app/build/outputs/bundle/release/app-release.aab'
[ -f "$unsigned_aab" ] || fail 'UNSIGNED_AAB=TOKEN_VAZIO'

signed_apk="${RUNNER_TEMP:-/tmp}/rafaelia-audio-studio-signed.apk"
apk_receipt="${RUNNER_TEMP:-/tmp}/rafaelia_signed_release_receipt_v1.txt"
signed_aab="${RUNNER_TEMP:-/tmp}/rafaelia-audio-studio-${RFA_VERSION_NAME}.aab"
aab_receipt="${RUNNER_TEMP:-/tmp}/rafaelia_signed_aab_receipt_v1.txt"

[ -f "$signed_apk" ] || fail 'SIGNED_APK_FROM_CANONICAL_RELEASE_NOT_FOUND'
[ -f "$apk_receipt" ] || fail 'SIGNED_APK_RECEIPT_NOT_FOUND'

bash ci/rafaelia-sign-aab.sh "$unsigned_aab" "$signed_aab" "$aab_receipt"

tag="rafaelia-signed-$RFA_CI_RUN_ID"
command -v gh >/dev/null 2>&1 || fail 'GH_CLI_UNAVAILABLE'
gh release upload "$tag" \
  "$signed_aab#Google Play Android App Bundle" \
  "$aab_receipt#Signed AAB receipt" \
  --repo "$GITHUB_REPOSITORY" \
  --clobber

echo 'SIGNED_AAB_RELEASE_ASSET=PASS'

if [ -n "${RFA_PAGES_REPO:-}" ] && [ -n "${RFA_PAGES_PAT:-${RFA_PAT_ACTIONS:-}}" ]; then
  bash ci/publish-update-page.sh \
    "$signed_apk" \
    "$signed_aab" \
    "$apk_receipt" \
    "$aab_receipt"
else
  echo 'UPDATE_PAGE_PUBLISH=TOKEN_VAZIO_PAGES_AUTHORITY'
fi

case "$mode" in
  store-internal)
    export RFA_PLAY_TRACK=internal
    export RFA_PLAY_STATUS=completed
    bash ci/publish-play.sh "$signed_aab"
    ;;
  store-production)
    export RFA_PLAY_TRACK=production
    export RFA_PLAY_STATUS=completed
    bash ci/publish-play.sh "$signed_aab"
    ;;
  signed-release)
    echo 'PLAY_PUBLISH=NOT_RUN_SIGNED_RELEASE_ONLY'
    ;;
esac

cat > "${RUNNER_TEMP:-/tmp}/rafaelia_distribution_receipt_v1.txt" <<EOF
schema=rafaelia.distribution/v1
source_sha=$RFA_SOURCE_SHA
run_id=$RFA_CI_RUN_ID
run_number=$RFA_CI_RUN_NUMBER
version_code=$RFA_VERSION_CODE
version_name=$RFA_VERSION_NAME
application_id=io.rafaelia.audiostudio
mode=$mode
signed_apk=PASS
signed_aab=PASS
pages_update_feed=$([ -n "${RFA_PAGES_REPO:-}" ] && printf 'ATTEMPTED' || printf 'TOKEN_VAZIO')
play_publish=$([ "$mode" = signed-release ] && printf 'NOT_RUN' || printf 'ATTEMPTED')
private_signing_material=SECRET_NOT_EXPORTED
claim_allowed=false
EOF

gh release upload "$tag" \
  "${RUNNER_TEMP:-/tmp}/rafaelia_distribution_receipt_v1.txt#Distribution receipt" \
  --repo "$GITHUB_REPOSITORY" \
  --clobber

echo 'DISTRIBUTION_RELEASE=PASS_EXECUTED_SCOPE'
