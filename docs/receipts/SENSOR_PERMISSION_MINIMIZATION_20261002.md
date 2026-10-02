# SENSOR_PERMISSION_MINIMIZATION — Receipt

## Identity

- schema: `rafaelia.sensor-permission-minimization-receipt/v1`
- repository: `rafaelmeloreisnovo/Est-dio-de-udio`
- canonical base: `b6f1ca67afc2b6a2adc9158078b0ac966bd99e84`
- pull request: `#23`
- implementation head before gate-cwd repair: `b80ed528c17408ba5f8052d7f090ebd9b8647b55`
- failed canonical START run: `36971780221`
- append-only: `true`

## Source finding

The implemented active μ∆ probes are accelerometer and magnetometer. They request `SensorManager.SENSOR_DELAY_GAME` and are bounded by time and sample count. No step-counter, step-detector or physical-activity classifier was found in the current source boundary.

The prior `ACTIVITY_RECOGNITION` manifest/request path therefore expanded permission surface without a current implemented feature requiring it.

## Materialized repair

- current manifest policy: `RECORD_AUDIO` + `ACCESS_NETWORK_STATE` only;
- `ACTIVITY_RECOGNITION` removed from manifest;
- old `REQ_ACTIVITY` request/result path removed from `MainActivity`;
- SYS state reports `SENSOR_MUDELTA=EXPLICIT_PROOF_ACTION`;
- `★ VALIDAR + ZIPRAF` discloses bounded accelerometer/magnetometer observation before probe registration;
- embedded permission contract updated;
- `ci/sensor-permission-gate.sh` created;
- gate bound into `:app:rafaeliaJavaGate` before APK completion;
- `SENSOR_PERMISSION_MATRIX_V3.md`, `SECURITY_BOUNDARIES.md`, and `START_HERE.md` updated;
- V2 permission matrix retained as historical predecessor.

## Negative evidence preserved

Canonical START run `36971780221` executed exact head `b80ed528c17408ba5f8052d7f090ebd9b8647b55` and failed at `40 · quality capability`.

Observed failure:

```text
:app:rafaeliaSensorPermissionGate FAILED
[SENSOR_PERMISSION_GATE][FAIL] MISSING=app/src/main/AndroidManifest.xml
```

Root cause was gate orchestration, not a discovered permission expansion: Gradle invoked the project-owned gate with current directory `app/`, while the new shell gate initially resolved repository paths relative to its caller. The successor repair makes the gate resolve repository root from `BASH_SOURCE[0]` before evaluating files.

This failed run is not overwritten or promoted to PASS.

## Required successor gate

A later exact-head START run must prove all of the following before merge:

```text
PERMISSION_SURFACE=RECORD_AUDIO,ACCESS_NETWORK_STATE
ACTIVITY_RECOGNITION=ABSENT
HIGH_SAMPLING_RATE_SENSORS=ABSENT
STEP_ACTIVITY_API=ABSENT
SENSOR_RATE_PROFILE=SENSOR_DELAY_GAME_BOUNDED_BY_PLATFORM
SENSOR_EVIDENCE_DISCLOSURE=EXPLICIT_PROOF_ACTION
SENSOR_PERMISSION_GATE=PASS_EXECUTED_SCOPE
```

The canonical pipeline must also complete its parser/ARM, architecture/ABI, evidence, deterministic build, static APK-installability and binary-receipt stages. Delivery jobs on the PR route must remain skipped.

## Evidence boundary

```text
SOURCE != EXECUTION != EVIDENCE != CLAIM
IMPLEMENTED_UNTESTED != PASS
TOKEN_VAZIO != 0
CI_GATE_PASS = PENDING_SUCCESSOR_EXECUTION
PHYSICAL_DEVICE_PERMISSION_FLOW = NOT_RUN
OEM_SENSOR_RATE_BEHAVIOR = NOT_RUN
EXTERNAL_STANDARD_AUDIT = NOT_AUDITED
claim_allowed = false
```

## Remaining uncertainty

1. Android physical-device permission prompts and denial/retry behavior are still `NOT_RUN` for this exact hotfix.
2. OEM sensor scheduling/rate behavior remains `NOT_RUN`.
3. Repository `main` provider protection remains a separate provider-control gap.
4. CI evidence does not establish physical sensor accuracy or causal interpretation of μ∆ observations.

## Rollback

Revert/close PR #23 only. No force-push, history rewrite, signing-key change, DSP/ABI/parser rollback or provider bypass is required.

## R3 at this receipt cut

`R3=<F_ok:permission surface minimized in source + fail-closed gate wired + first orchestration defect exposed,F_gap:successor CI + physical Android/OEM checks,F_next:exact-head successor START then bounded merge/post-merge readback>`
