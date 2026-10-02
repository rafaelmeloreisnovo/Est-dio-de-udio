# Rafaelia Audio · Distribution / Update V1

## Intent

Reduce Android install friction without weakening provenance, signing or provider gates.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
```

## Android update identity

The package identity remains:

```text
applicationId=io.rafaelia.audiostudio
minSdk=29
targetSdk=36
```

CI derives `versionCode` deterministically from the exact source commit timestamp:

```text
versionCode = commit_epoch_seconds - 2020-01-01T00:00:00Z
versionName = r<versionCode>-<source_sha_8>
```

This makes the Android revision source-bound rather than CI-run-bound.

An Android in-place update still requires a compatible installed signing certificate. A debug-signed installation cannot be promoted into an authorially signed installation by changing only `versionCode`.

## One-time signer migration

The current debug/non-authorial line and the authorial distribution line are distinct signing identities unless proven otherwise.

Therefore:

```text
DEBUG_CERT != AUTHORIAL_CERT  -> one final uninstall/reinstall may be required
AUTHORIAL_CERT == AUTHORIAL_CERT + higher versionCode -> normal in-place update path
```

Do not claim `NO_UNINSTALL_REQUIRED` until the exact installed certificate matches the authorial app-signing certificate.

## Google Play signing invariant

For cross-store compatibility between a directly downloaded authorial APK and Google Play, configure Play App Signing with the project-owned app signing key rather than allowing a different Google-generated app signing identity.

A separate upload key is recommended for AAB uploads, but the APK installed on devices must ultimately be signed by the same app signing identity used by the direct-distribution APK, or by a platform-valid key-rotation lineage.

Play Console enrollment / Terms / app-signing-key configuration is a provider action and is not inferred from a successful AAB upload.

## Delivery modes

The single canonical workflow exposes:

```text
validate
live-debug
provider-bootstrap
signed-release
store-internal
store-production
```

### signed-release

Builds and publishes:

- authorially signed APK;
- authorially signed AAB;
- signing receipt;
- AAB receipt;
- distribution receipt;
- optional Pages update feed when Pages authority is configured.

### store-internal

Runs the full signed-release path and then publishes the AAB to the Google Play `internal` track through Android Publisher API v3.

### store-production

Same mechanism, but `production` is fail-closed unless:

```text
RAFAELIA_PLAY_PRODUCTION_APPROVED=true
```

This prevents an accidental public rollout.

## GitHub Pages update feed

The Pages publisher writes:

```text
docs/site/rafaelia-audio/index.html
docs/site/rafaelia-audio/latest.json
docs/site/rafaelia-audio/signing-latest.txt
docs/site/rafaelia-audio/aab-latest.txt
```

The JSON feed binds:

- application ID;
- version code/name;
- exact source SHA;
- CI run;
- release tag;
- APK/AAB URLs;
- APK/AAB SHA-256;
- signing certificate SHA-256.

No private key material is exported.

## GitHub capability tokens

Tokens are routed by capability and are never printed:

| Capability | Secret name | Intended scope in this design |
|---|---|---|
| provider environment / policy | `PAT_ENV`, `PAT_ENVIRONMENTS` | non-delete provider governance / environments |
| Actions / Pages publication | `PAT_ACTIONS` | release/update publication edge when required |
| agent capability | `PAT_AGENTS` | declared capability; not consumed by distribution path |
| Dependabot capability | `PAT_DEPENDABOT` | declared capability; not consumed by distribution path |
| Codespaces capability | `PAT_CODESPACE` | declared capability; not consumed by distribution path |

Possession of a secret name is not evidence that the token exists or has the expected provider permission. Provider readback is required.

## Provider bootstrap

`provider-bootstrap` performs only non-delete governance actions:

- protects `main`;
- requires `gate / build` before promotion;
- blocks force pushes;
- blocks deletion;
- creates/updates `signed-release`, `google-play`, and `github-pages` environments.

No delete endpoint is called.

## Play authority required

GitHub PATs do not authorize Google Play. Store publication additionally requires:

```text
RAFAELIA_PLAY_SERVICE_ACCOUNT_B64
```

The service account must already have appropriate access to the app in Play Console.

Recommended variables:

```text
RAFAELIA_PLAY_PACKAGE_NAME=io.rafaelia.audiostudio
RAFAELIA_PLAY_PRODUCTION_APPROVED=false
```

## Current claim boundary

Until exact provider executions are performed:

```text
UPDATE_PIPELINE=IMPLEMENTED_UNTESTED
PLAY_PUBLISH=NOT_RUN
PAGES_UPDATE_FEED=NOT_RUN
PROVIDER_BOOTSTRAP=NOT_RUN
PHYSICAL_IN_PLACE_UPDATE=NOT_RUN
```

Promotion to PASS occurs only from execution evidence.
