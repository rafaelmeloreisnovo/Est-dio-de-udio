#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_REPRO_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

apk='app/build/outputs/apk/debug/app-debug.apk'
test -f "$apk" || fail 'BUILD_A_APK=TOKEN_VAZIO'

work="${RUNNER_TEMP:-/tmp}/rafaelia-reproducible-debug"
rm -rf "$work"
mkdir -p "$work/a" "$work/b"

cp "$apk" "$work/a/app-debug.apk"
sha_a="$(sha256sum "$work/a/app-debug.apk" | awk '{print $1}')"

# Rebuild the exact same source in the same pinned CI/toolchain environment after
# deleting generated Gradle outputs. This is an intra-environment reproducibility
# gate; it is not an independent-environment reproduction claim.
gradle clean :app:rafaeliaDebugApk --no-daemon --stacktrace

test -f "$apk" || fail 'BUILD_B_APK=TOKEN_VAZIO'
cp "$apk" "$work/b/app-debug.apk"
sha_b="$(sha256sum "$work/b/app-debug.apk" | awk '{print $1}')"

printf 'REPRO_BUILD_A_SHA256=%s\n' "$sha_a"
printf 'REPRO_BUILD_B_SHA256=%s\n' "$sha_b"

if test "$sha_a" != "$sha_b"; then
  for side in a b; do
    : > "$work/$side/entries.sha256"
    while IFS= read -r entry; do
      case "$entry" in
        */) continue ;;
      esac
      digest="$(unzip -p "$work/$side/app-debug.apk" "$entry" | sha256sum | awk '{print $1}')"
      printf '%s  %s\n' "$digest" "$entry" >> "$work/$side/entries.sha256"
    done < <(unzip -Z1 "$work/$side/app-debug.apk" | LC_ALL=C sort)
  done

  echo 'REPRO_ENTRY_DIFF_BEGIN'
  diff -u "$work/a/entries.sha256" "$work/b/entries.sha256" || true
  echo 'REPRO_ENTRY_DIFF_END'
  fail "APK_REPRODUCIBILITY=MISMATCH build_a=$sha_a build_b=$sha_b"
fi

cmp -s "$work/a/app-debug.apk" "$work/b/app-debug.apk" || \
  fail 'APK_BYTE_IDENTITY=MISMATCH_WITH_EQUAL_SHA_UNEXPECTED'

echo 'APK_REPRODUCIBLE_SAME_ENV=PASS'
echo 'REPRO_SCOPE=SAME_SOURCE_SAME_PINNED_CI_ENVIRONMENT'
echo 'INDEPENDENT_REPRODUCTION=NOT_CLAIMED'
echo 'EXTERNAL_STANDARD_AUDIT=NOT_AUDITED'
echo 'CLAIM_ALLOWED=false'
