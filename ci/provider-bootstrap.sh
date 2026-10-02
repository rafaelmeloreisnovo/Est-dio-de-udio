#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_PROVIDER_BOOTSTRAP][FAIL] %s\n' "$*" >&2
  exit 1
}

repo="${GITHUB_REPOSITORY:-}"
token="${RFA_PROVIDER_ADMIN_TOKEN:-${RFA_PAT_ENVIRONMENTS:-${RFA_PAT_ENV:-${RFA_PAT_ACTIONS:-}}}}"
[ -n "$repo" ] || fail 'GITHUB_REPOSITORY_TOKEN_VAZIO'
[ -n "$token" ] || fail 'PROVIDER_ADMIN_TOKEN=TOKEN_VAZIO'
command -v gh >/dev/null 2>&1 || fail 'GH_CLI_UNAVAILABLE'

export GH_TOKEN="$token"

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

printf 'PROVIDER_MAIN_PROTECTION=PASS_READBACK\n'
printf 'PROVIDER_REQUIRED_CONTEXT=gate / build\n'
printf 'PROVIDER_FORCE_PUSH=BLOCKED\n'
printf 'PROVIDER_DELETE=BLOCKED\n'
printf 'PROVIDER_MUTATION_SCOPE=NON_DELETE_ONLY\n'
printf 'PROVIDER_TOKEN_VALUE=SECRET_NOT_EXPORTED\n'
