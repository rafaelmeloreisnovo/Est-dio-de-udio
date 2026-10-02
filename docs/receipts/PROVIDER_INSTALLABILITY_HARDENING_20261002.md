# Provider + APK installability hardening — 2026-10-02

## Intent

Reduce the two highest-return risks without conflating source/build evidence with a physical Android install.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
INSTALLABLE_STATIC != INSTALLED_PHYSICAL
IMPLEMENTED_UNTESTED != PASS
```

## Fresh provider readback

- repository rulesets endpoint returned an empty list during this work;
- the branch-protection endpoint is not readable by the installed GitHub integration (`403 Resource not accessible by integration`), so this session cannot administer server-side branch protection;
- PR #18 records a prior provider readback with `main protected=false` and already changed `push(main)` to validate-only;
- live prerelease `rafaelia-live-36952616484` declares `signing_mode=DEBUG_NONAUTHORIAL` and is therefore not an authorial signed release.

Provider-side branch protection/ruleset activation remains an external administrative action until the provider reports the ref as protected.

## Source hardening implemented

### Signed-release provider gate

`ci/provider-enforcement-gate.sh` requires all of the following before an authorial signed release can proceed:

```text
RFA_PROVIDER_REF=refs/heads/main
RFA_PROVIDER_REF_PROTECTED=true
RFA_SOURCE_SHA == RFA_PROVIDER_REF_SHA
```

The values are supplied from the GitHub Actions event context. A missing/unprotected/non-main ref fails closed before signing or publication.

### Static APK installability gate

`ci/apk-installability-static.sh` checks the exact APK for:

- ZIP/container integrity;
- Android signature verification;
- zip alignment;
- package id `io.rafaelia.audiostudio`;
- `minSdk=29`;
- `targetSdk=35`;
- packaged `armeabi-v7a` native library;
- packaged `arm64-v8a` native library.

The canonical PR/main gate runs this after the debug APK build. Live-debug delivery repeats the check on the exact binary before publication. Authorial signing invokes the same check on the exact signed APK before its receipt is emitted.

## Claim boundary

```text
PROVIDER_RULESET_READBACK=[]
CLASSIC_BRANCH_PROTECTION_WRITE=BLOCKED_BY_CONNECTOR_CAPABILITY
REAL_SIGNING_KEY_CONFIGURATION=TOKEN_VAZIO_FROM_THIS_SESSION
INSTALLABLE_STATIC=PENDING_CI
INSTALLED_ANDROID10=NOT_RUN
OGG_OPUS_PHYSICAL_ROUNDTRIP=NOT_RUN
WHATSAPP_REAL_ROUNDTRIP=NOT_RUN
CFR_PHYSICAL_REPEATABILITY=NOT_RUN
claim_allowed=false
```

Static installability reduces packaging risk but cannot prove that `adb install` succeeds on a particular device. Device state, an already-installed package signed by a different certificate, storage, OEM policy and runtime behavior remain physical/device gates.

## Physical next gate

On Android 10 / API 29, preserve the exact APK SHA-256 and record each install attempt separately. For repeated acoustic tests, record device, build SHA, APK SHA, capture source, trial number, noise floor and measured outputs. Do not promote physical claims from CI-only evidence.

R3=<F_ok: repository fail-closed provider gate + static installability gate materialized; F_gap: provider protection activation + real authorial keystore + Android 10 physical install/roundtrip; F_next: CI exact-head proof, then physical install receipt on API29>
