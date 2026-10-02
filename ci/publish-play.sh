#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '[RAFAELIA_PLAY][FAIL] %s\n' "$*" >&2
  exit 1
}

signed_aab="${1:-}"
[ -f "$signed_aab" ] || fail 'SIGNED_AAB_NOT_FOUND'

: "${RFA_PLAY_SERVICE_ACCOUNT_B64:?PLAY_SERVICE_ACCOUNT_B64_TOKEN_VAZIO}"
package_name="${RFA_PLAY_PACKAGE_NAME:-io.rafaelia.audiostudio}"
track="${RFA_PLAY_TRACK:-internal}"
status="${RFA_PLAY_STATUS:-completed}"

case "$track" in
  internal|alpha|beta|production) ;;
  *) fail "PLAY_TRACK_INVALID=$track" ;;
esac
case "$status" in
  completed|draft|inProgress|halted) ;;
  *) fail "PLAY_STATUS_INVALID=$status" ;;
esac
if [ "$track" = production ] && [ "${RFA_PLAY_PRODUCTION_APPROVED:-false}" != true ]; then
  fail 'PLAY_PRODUCTION_APPROVED=false'
fi

command -v curl >/dev/null 2>&1 || fail 'CURL_NOT_FOUND'
command -v openssl >/dev/null 2>&1 || fail 'OPENSSL_NOT_FOUND'
command -v python3 >/dev/null 2>&1 || fail 'PYTHON3_NOT_FOUND'

tmp="${RUNNER_TEMP:-/tmp}/rafaelia-play-$$"
rm -rf "$tmp"
mkdir -p "$tmp"
trap 'rm -rf "$tmp"' EXIT
sa="$tmp/service-account.json"
key="$tmp/private-key.pem"

printf '%s' "$RFA_PLAY_SERVICE_ACCOUNT_B64" | base64 --decode > "$sa"
[ -s "$sa" ] || fail 'PLAY_SERVICE_ACCOUNT_DECODE_EMPTY'

client_email="$(python3 - "$sa" <<'PY'
import json,sys
with open(sys.argv[1], encoding='utf-8') as f: print(json.load(f).get('client_email',''))
PY
)"
python3 - "$sa" "$key" <<'PY'
import json,sys
with open(sys.argv[1], encoding='utf-8') as f: data=json.load(f)
key=data.get('private_key','')
if not key: raise SystemExit(2)
with open(sys.argv[2],'w',encoding='utf-8') as f: f.write(key)
PY
[ -n "$client_email" ] || fail 'PLAY_CLIENT_EMAIL_TOKEN_VAZIO'
[ -s "$key" ] || fail 'PLAY_PRIVATE_KEY_TOKEN_VAZIO'
chmod 600 "$key"

now="$(date +%s)"
exp=$((now + 3300))
header='{"alg":"RS256","typ":"JWT"}'
claims="$(printf '{\"iss\":\"%s\",\"scope\":\"https://www.googleapis.com/auth/androidpublisher\",\"aud\":\"https://oauth2.googleapis.com/token\",\"iat\":%s,\"exp\":%s}' "$client_email" "$now" "$exp")"

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
unsigned="$(printf '%s' "$header" | b64url).$(printf '%s' "$claims" | b64url)"
sig="$(printf '%s' "$unsigned" | openssl dgst -sha256 -sign "$key" | b64url)"
assertion="$unsigned.$sig"

token_json="$(curl --fail --silent --show-error \
  -X POST 'https://oauth2.googleapis.com/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer' \
  --data-urlencode "assertion=$assertion")"
access_token="$(printf '%s' "$token_json" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))')"
[ -n "$access_token" ] || fail 'PLAY_OAUTH_ACCESS_TOKEN_TOKEN_VAZIO'

auth="Authorization: Bearer $access_token"
api="https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$package_name"
upload_api="https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$package_name"

edit_json="$(curl --fail --silent --show-error -X POST -H "$auth" -H 'Content-Type: application/json' -d '{}' "$api/edits")"
edit_id="$(printf '%s' "$edit_json" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))')"
[ -n "$edit_id" ] || fail 'PLAY_EDIT_ID_TOKEN_VAZIO'

bundle_json="$(curl --fail --silent --show-error -X POST -H "$auth" -H 'Content-Type: application/octet-stream' --data-binary "@$signed_aab" "$upload_api/edits/$edit_id/bundles?uploadType=media")"
play_version_code="$(printf '%s' "$bundle_json" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("versionCode",""))')"
[ -n "$play_version_code" ] || fail 'PLAY_BUNDLE_VERSION_CODE_TOKEN_VAZIO'

release_name="Rafaelia ${RFA_VERSION_NAME:-$play_version_code}"
track_body="$(python3 - "$release_name" "$play_version_code" "$status" <<'PY'
import json,sys
print(json.dumps({"releases":[{"name":sys.argv[1],"versionCodes":[sys.argv[2]],"status":sys.argv[3]}]}, separators=(',',':')))
PY
)"

curl --fail --silent --show-error -X PUT -H "$auth" -H 'Content-Type: application/json' -d "$track_body" "$api/edits/$edit_id/tracks/$track" >/dev/null
commit_json="$(curl --fail --silent --show-error -X POST -H "$auth" -H 'Content-Type: application/json' "$api/edits/$edit_id:commit")"
committed_id="$(printf '%s' "$commit_json" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))')"
[ -n "$committed_id" ] || fail 'PLAY_EDIT_COMMIT_TOKEN_VAZIO'

printf 'PLAY_PUBLISH=PASS_EXECUTED_SCOPE\n'
printf 'PLAY_PACKAGE=%s\n' "$package_name"
printf 'PLAY_TRACK=%s\n' "$track"
printf 'PLAY_STATUS=%s\n' "$status"
printf 'PLAY_VERSION_CODE=%s\n' "$play_version_code"
printf 'PLAY_PRIVATE_CREDENTIAL=SECRET_NOT_EXPORTED\n'
