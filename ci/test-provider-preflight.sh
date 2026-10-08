#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

fail() { printf 'PROVIDER_PRETEST_FAIL=%s\n' "$*" >&2; exit 1; }

workflow='.github/workflows/START.yml'
bootstrap='ci/provider-bootstrap.sh'
test -s "$workflow" && test -s "$bootstrap" || fail 'SOURCE_MISSING'

# Verify manual owner-only capability in the *active* pipeline.
grep -Fq "github.event_name == 'workflow_dispatch' && github.actor == 'rafaelmeloreisnovo' && github.ref == 'refs/heads/main' && needs.plan.outputs.delivery == 'provider-bootstrap'" "$workflow" || fail 'ADMIN_JOB_NOT_OWNER_BOUND'
grep -Fq 'RFA_PROVIDER_ADMIN_TOKEN: ${{ secrets.PAT_ENVIRONMENTS }}' "$workflow" || fail 'DEDICATED_PAT_MISSING'
grep -Fq 'name: provider-admin' "$workflow" || fail 'PROVIDER_ENVIRONMENT_NOT_SEPARATED'
if grep -Eq 'secrets.PAT_ACTIONS|secrets.PAT_ENV[[:space:]]*\|\|' "$workflow"; then
  fail 'ADMIN_OR_RELEASE_USES_CROSS_CAPABILITY_PAT_FALLBACK'
fi
grep -Fq 'name: 04 · provider / verify main' "$workflow" || fail 'MAIN_READBACK_NOT_SEPARATED'
grep -Fq 'P0_MAIN_UNPROTECTED' "$workflow" || fail 'MAIN_PROTECTION_MISSING_NOT_FAIL_CLOSED'
grep -Fq 'MAIN_REQUIRED_CHECK_OR_NONDELETE_ENFORCEMENT_READBACK_FAIL' "$bootstrap" || fail 'ADMIN_POLICY_READBACK_MISSING'
grep -Fq 'MAIN_MOVED_BEFORE_ADMIN_MUTATION' "$bootstrap" || fail 'CURRENT_HEAD_NOT_BOUND'

# Test preflight denials without network, gh, repo secrets, or repository writes.
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
sha='0123456789abcdef0123456789abcdef01234567'
expect_block() {
  local marker="$1"
  shift
  if env -i PATH="$PATH" GITHUB_REPOSITORY='rafaelmeloreisnovo/EstudioAudio' \
      GITHUB_EVENT_NAME=workflow_dispatch GITHUB_ACTOR=rafaelmeloreisnovo \
      GITHUB_REF=refs/heads/main GITHUB_SHA="$sha" RFA_SOURCE_SHA="$sha" \
      "$@" bash "$bootstrap" > "$tmp/out" 2>&1; then
    fail "UNAUTHORIZED_SUCCEEDED:$marker"
  fi
  grep -Fq "$marker" "$tmp/out" || fail "WRONG_BLOCK_REASON:$marker"
}
expect_block 'ADMIN_MUTATION_MANUAL_ONLY' GITHUB_EVENT_NAME=push
expect_block 'ADMIN_MUTATION_OWNER_ONLY' GITHUB_ACTOR=unknown_actor
expect_block 'ADMIN_MUTATION_MAIN_ONLY' GITHUB_REF=refs/heads/feature
expect_block 'EXACT_REF_SHA_MISMATCH' GITHUB_SHA=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
expect_block 'PAT_ENVIRONMENTS=TOKEN_VAZIO' RFA_PROVIDER_ADMIN_TOKEN=

printf 'PROVIDER_ADMIN_BOUNDARY=PASS_NEGATIVE_TESTS\n'
printf 'PROVIDER_BRANCH_ENFORCEMENT=TOKEN_VAZIO_UNTIL_REAL_GITHUB_READBACK\n'
