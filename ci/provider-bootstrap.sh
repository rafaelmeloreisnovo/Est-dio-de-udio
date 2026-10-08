#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_PROVIDER_BOOTSTRAP][FAIL] %s\n' "$*" >&2
  exit 1
}

repo="${GITHUB_REPOSITORY:-}"
token="${RFA_PROVIDER_ADMIN_TOKEN:-}"
[ -n "$repo" ] || fail 'GITHUB_REPOSITORY_TOKEN_VAZIO'
[ "${GITHUB_EVENT_NAME:-}" = workflow_dispatch ] || fail 'ADMIN_MUTATION_MANUAL_ONLY'
[ "${GITHUB_ACTOR:-}" = rafaelmeloreisnovo ] || fail 'ADMIN_MUTATION_OWNER_ONLY'
[ "${GITHUB_REF:-}" = refs/heads/main ] || fail 'ADMIN_MUTATION_MAIN_ONLY'
[[ "${RFA_SOURCE_SHA:-}" =~ ^[0-9a-f]{40}$ ]] || fail 'SOURCE_SHA_INVALID'
[ "${GITHUB_SHA:-}" = "$RFA_SOURCE_SHA" ] || fail 'EXACT_REF_SHA_MISMATCH'
[ -n "$token" ] || fail 'PAT_ENVIRONMENTS=TOKEN_VAZIO — no Actions/admin fallback'
command -v gh >/dev/null 2>&1 || fail 'GH_CLI_UNAVAILABLE'

# The administrative credential exists only in the owner-only manual job.
# It is not copied to the APK or used for routine main/PR build gates.
export GH_TOKEN="$token"

head_sha="$(gh api "repos/$repo/branches/main" --jq '.commit.sha')"
[ "$head_sha" = "$RFA_SOURCE_SHA" ] || fail 'MAIN_MOVED_BEFORE_ADMIN_MUTATION'
printf 'PROVIDER_PREFLIGHT=PASS_OWNER_MANUAL_EXACT_HEAD\n'

# Non-destructive provider governance only. No delete endpoint is called.
cat > "${RUNNER_TEMP:-/tmp}/rafaelia-main-protection.json" <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["gate / build"]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": null,
  "restrictions": null,
  "required_linear_history": false,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true,
  "lock_branch": false,
  "allow_fork_syncing": true
}
JSON

gh api \
  --method PUT \
  -H 'Accept: application/vnd.github+json' \
  "repos/$repo/branches/main/protection" \
  --input "${RUNNER_TEMP:-/tmp}/rafaelia-main-protection.json" >/dev/null

for environment in signed-release google-play github-pages; do
  printf '{}' | gh api \
    --method PUT \
    -H 'Accept: application/vnd.github+json' \
    "repos/$repo/environments/$environment" \
    --input - >/dev/null
  printf 'PROVIDER_ENVIRONMENT_%s=PASS\n' "$(printf '%s' "$environment" | tr 'a-z-' 'A-Z_')"
done

protected="$(gh api "repos/$repo/branches/main" --jq '.protected')"
[ "$protected" = true ] || fail "MAIN_PROTECTED_READBACK=$protected"

# Branch readback alone cannot prove enforcement of the exact required check.
# Verify policy objects with the authorized admin credential.
policy="$(gh api "repos/$repo/branches/main/protection")"
printf '%s' "$policy" | jq -e '
  .required_status_checks.strict == true and
  ((.required_status_checks.contexts // []) | index("gate / build") != null) and
  .enforce_admins.enabled == true and
  .allow_force_pushes.enabled == false and
  .allow_deletions.enabled == false and
  .required_conversation_resolution.enabled == true
' >/dev/null || fail 'MAIN_REQUIRED_CHECK_OR_NONDELETE_ENFORCEMENT_READBACK_FAIL'

printf 'PROVIDER_MAIN_PROTECTION=PASS_READBACK\n'
printf 'PROVIDER_REQUIRED_CHECK_VERIFIED=gate / build\n'
printf 'PROVIDER_REQUIRED_CONTEXT=gate / build\n'
printf 'PROVIDER_FORCE_PUSH=BLOCKED\n'
printf 'PROVIDER_DELETE=BLOCKED\n'
printf 'PROVIDER_MUTATION_SCOPE=NON_DELETE_ONLY\n'
printf 'PROVIDER_TOKEN_VALUE=SECRET_NOT_EXPORTED\n'
