#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_UPDATE_PAGE][FAIL] %s\n' "$*" >&2
  exit 1
}

repo="${RFA_PAGES_REPO:-}"
token="${RFA_PAGES_PAT:-${RFA_PAT_ACTIONS:-}}"
signed_apk="${1:-}"
signed_aab="${2:-}"
signing_receipt="${3:-}"
aab_receipt="${4:-}"

[ -n "$repo" ] || fail 'PAGES_REPO=TOKEN_VAZIO'
[ -n "$token" ] || fail 'PAGES_TOKEN=TOKEN_VAZIO'
[ -f "$signed_apk" ] || fail 'SIGNED_APK_NOT_FOUND'
[ -f "$signed_aab" ] || fail 'SIGNED_AAB_NOT_FOUND'
[ -f "$signing_receipt" ] || fail 'SIGNING_RECEIPT_NOT_FOUND'
[ -f "$aab_receipt" ] || fail 'AAB_RECEIPT_NOT_FOUND'
: "${RFA_SOURCE_SHA:?SOURCE_SHA_TOKEN_VAZIO}"
: "${RFA_CI_RUN_ID:?CI_RUN_ID_TOKEN_VAZIO}"
: "${RFA_VERSION_CODE:?VERSION_CODE_TOKEN_VAZIO}"
: "${RFA_VERSION_NAME:?VERSION_NAME_TOKEN_VAZIO}"

command -v gh >/dev/null 2>&1 || fail 'GH_CLI_UNAVAILABLE'

apk_sha="$(sha256sum "$signed_apk" | awk '{print $1}')"
aab_sha="$(sha256sum "$signed_aab" | awk '{print $1}')"
cert_sha="$(awk -F= '$1=="observed_certificate_sha256"{print $2; exit}' "$signing_receipt")"
tag="rafaelia-signed-$RFA_CI_RUN_ID"
release_base="https://github.com/$GITHUB_REPOSITORY/releases/download/$tag"
apk_name="$(basename "$signed_apk")"
aab_name="$(basename "$signed_aab")"
apk_url="$release_base/$apk_name"
aab_url="$release_base/$aab_name"

work="${RUNNER_TEMP:-/tmp}/rafaelia-update-page-$$"
rm -rf "$work"
mkdir -p "$work"
trap 'rm -rf "$work"' EXIT

export GH_TOKEN="$token"
gh auth setup-git >/dev/null 2>&1 || true
gh repo clone "$repo" "$work/repo" -- --depth=1
cd "$work/repo"

git checkout main
mkdir -p docs/site/rafaelia-audio

cat > docs/site/rafaelia-audio/latest.json <<EOF
{
  "schema": "rafaelia.update-feed/v1",
  "application_id": "io.rafaelia.audiostudio",
  "version_code": $RFA_VERSION_CODE,
  "version_name": "$RFA_VERSION_NAME",
  "source_sha": "$RFA_SOURCE_SHA",
  "run_id": "$RFA_CI_RUN_ID",
  "release_tag": "$tag",
  "apk_url": "$apk_url",
  "apk_sha256": "$apk_sha",
  "aab_url": "$aab_url",
  "aab_sha256": "$aab_sha",
  "certificate_sha256": "$cert_sha",
  "update_policy": "same_application_id+same_signing_certificate+higher_version_code",
  "claim_allowed": false
}
EOF

cat > docs/site/rafaelia-audio/index.html <<EOF
<!doctype html>
<html lang="pt-BR">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Rafaelia Audio Studio · Atualização</title></head>
<body>
  <main>
    <h1>Rafaelia Audio Studio</h1>
    <p>Versão <strong>$RFA_VERSION_NAME</strong> · versionCode <strong>$RFA_VERSION_CODE</strong></p>
    <p>Source SHA: <code>$RFA_SOURCE_SHA</code></p>
    <p><a href="$apk_url">Baixar APK assinado</a></p>
    <p><a href="$aab_url">Android App Bundle (AAB)</a></p>
    <p>APK SHA-256: <code>$apk_sha</code></p>
    <p>Certificado SHA-256: <code>$cert_sha</code></p>
    <p>Atualizações Android exigem o mesmo applicationId, certificado compatível e versionCode não regressivo.</p>
  </main>
</body>
</html>
EOF

cp "$signing_receipt" docs/site/rafaelia-audio/signing-latest.txt
cp "$aab_receipt" docs/site/rafaelia-audio/aab-latest.txt

git config user.name 'rafaelia-distribution-bot'
git config user.email 'actions@users.noreply.github.com'
git add docs/site/rafaelia-audio
if git diff --cached --quiet; then
  echo 'UPDATE_PAGE_NO_CHANGE=PASS'
  exit 0
fi

git commit -m "release: publish Rafaelia Audio $RFA_VERSION_NAME update feed"
if git push origin HEAD:main; then
  echo 'UPDATE_PAGE_PUBLISH=PASS_DIRECT_MAIN'
else
  branch="release/rafaelia-audio-update-$RFA_CI_RUN_ID"
  git checkout -b "$branch"
  git push origin "$branch"
  gh pr create \
    --repo "$repo" \
    --base main \
    --head "$branch" \
    --title "release: Rafaelia Audio $RFA_VERSION_NAME update feed" \
    --body "Automated update feed. No private key material is exported."
  echo 'UPDATE_PAGE_PUBLISH=PENDING_PR'
fi
