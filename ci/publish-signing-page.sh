#!/usr/bin/env bash
set -euo pipefail

receipt="${1:-}"
repo="${RFA_PAGES_REPO:-}"
token="${RFA_PAGES_PAT:-}"

[ -f "$receipt" ] || {
  echo "PAGES_RECEIPT_NOT_FOUND"
  exit 1
}

if [ -z "$repo" ] || [ -z "$token" ]; then
  echo "PAGES_PUBLISH=TOKEN_VAZIO"
  exit 0
fi

command -v gh >/dev/null 2>&1 || {
  echo "GH_CLI_UNAVAILABLE"
  exit 1
}

tmp="${RUNNER_TEMP:-/tmp}/rafaelia-pages-$$"
rm -rf "$tmp"
mkdir -p "$tmp"
trap 'rm -rf "$tmp"' EXIT

export GH_TOKEN="$token"
gh repo clone "$repo" "$tmp/repo" -- --depth=1
cd "$tmp/repo"

branch="evidence/rafaelia-audio-sign-${RFA_CI_RUN_ID:-manual}"
git checkout -b "$branch"
mkdir -p docs/site/rafaelia-signing
cp "$receipt" docs/site/rafaelia-signing/latest.txt

source_sha="$(awk -F= '$1=="source_sha"{print $2}' "$receipt")"
signed_sha="$(awk -F= '$1=="signed_apk_sha256"{print $2}' "$receipt")"
cert_sha="$(awk -F= '$1=="observed_certificate_sha256"{print $2}' "$receipt")"
signer_id="$(awk -F= '$1=="signer_id"{print $2}' "$receipt")"

cat > docs/site/rafaelia-signing/latest.json <<EOF
{
  "schema": "rafaelia.signed-release/v1",
  "source_sha": "$source_sha",
  "signed_apk_sha256": "$signed_sha",
  "certificate_sha256": "$cert_sha",
  "signer_id": "$signer_id",
  "certificate_match": "PASS"
}
EOF

git config user.name "rafaelia-signing-bot"
git config user.email "actions@users.noreply.github.com"
git add docs/site/rafaelia-signing/latest.txt docs/site/rafaelia-signing/latest.json
git commit -m "evidence: update Rafaelia Audio signing receipt"
git push origin "$branch"

gh pr create   --repo "$repo"   --base main   --head "$branch"   --title "evidence: publish Rafaelia Audio signing receipt"   --body "Automated public signing receipt. Private signing material is not exported."

echo "PAGES_PUBLISH_PR=CREATED"
