# Signed Release V1

## Goal

Produce an APK whose installed signing certificate can be verified against a public expected SHA-256.

## Required public variables

```text
RAFAELIA_SIGNER_ID
RAFAELIA_EXPECTED_CERT_SHA256
RAFAELIA_ANDROID_KEY_ALIAS
RAFAELIA_PAGES_REPO
```

## Required Secrets

```text
RAFAELIA_ANDROID_KEYSTORE_B64
RAFAELIA_ANDROID_STORE_PASSWORD
RAFAELIA_ANDROID_KEY_PASSWORD
RAFAELIA_PAGES_PAT
```

The first three are required for signing. The Pages PAT is only required for cross-repository publication of the public receipt.

## Flow

```text
exact source SHA
-> require canonical gate PASS
-> authorial toolchain bootstrap
-> component-origin + permission assets
-> unsigned release APK
-> apksigner
-> observed certificate SHA-256
-> compare to expected certificate SHA-256
-> signed APK SHA-256
-> signed release receipt
-> GitHub prerelease
-> governed PR to RafGitTools Pages source
```

## Fail-closed conditions

Signing fails if:

- keystore is absent;
- password is absent;
- key alias is absent;
- expected certificate fingerprint is absent or malformed;
- observed certificate differs from the expected fingerprint;
- source SHA does not already have a successful canonical gate.

## Runtime verification

The installed application independently reads its own signing certificate SHA-256 using Android PackageManager and writes:

```text
installed_signing_cert_sha256
expected_signing_cert_sha256
signing_cert_matches_expected
```

This allows:

```text
signed release receipt
<-> installed APK certificate
<-> installed APK SHA-256
```

## Boundary

```text
SIGNATURE_MATCH = authentication evidence
SIGNATURE_MATCH != scientific validation
SIGNATURE_MATCH != ownership of external platform/toolchain code
```
