# Receipt — explicit YAML compile gate — 2026-10-02

## Intent

Make compilation an explicit, visible and fail-closed phase of `.github/workflows/START.yml` instead of relying only on compilation hidden behind higher-level Gradle tasks.

## Source cut

- base main: `4a62742323fef789d07e7d277846387f36a68423`
- workflow: `.github/workflows/START.yml`
- new gate: `ci/compile-debug-gate.sh`

## Executed scope required by the gate

The workflow step `42 · explicit compile / Java + native ABIs` must execute:

- `:app:compileDebugJavaWithJavac`
- `:app:externalNativeBuildDebug`
- proof that `MainActivity.class` exists under `app/build`
- proof that `librafaelia_audio.so` exists for `armeabi-v7a`
- proof that `librafaelia_audio.so` exists for `arm64-v8a`

Any missing compiler output is a hard FAIL before evidence preparation or APK packaging.

## Boundary

`SOURCE != EXECUTION != EVIDENCE != CLAIM`

At commit time this receipt is `IMPLEMENTED_UNTESTED` until the canonical START workflow succeeds on the exact PR head.

`PHYSICAL_DEVICE_EXECUTION=NOT_RUN`

`EXTERNAL_STANDARD_AUDIT=NOT_AUDITED`

`CLAIM_ALLOWED=false`

## Rollback

Revert the workflow step and `ci/compile-debug-gate.sh`; no DSP, ABI, permission, signing-key or provider-policy changes are part of this delta.
