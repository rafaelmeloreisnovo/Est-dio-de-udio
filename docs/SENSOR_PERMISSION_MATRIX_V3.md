# Sensor Permission Matrix V3

Status: **implemented on hotfix branch; physical-device permission flow remains NOT_RUN until device execution.**

Supersedes for current operational guidance: `SENSOR_PERMISSION_MATRIX_V2.md`.
The V2 file is retained as historical evidence and is not rewritten.

## Invariant

```text
permission declared != capability present != sensor sampled != evidence valid != claim
TOKEN_VAZIO != PASS
REFERENCE_TO_STANDARD != AUDITED_CONFORMITY
```

## Current Android permission surface

| Capability | Android permission | Current policy |
|---|---|---|
| microphone recording / acoustic calibration | `RECORD_AUDIO` | dangerous permission; request only from explicit REC/CAL/narration action |
| active-network transport metadata | `ACCESS_NETWORK_STATE` | normal permission; passive platform metadata only |
| accelerometer | none | local sensor; bounded observation only from explicit `★ VALIDAR + ZIPRAF` action |
| magnetometer | none | local sensor; bounded observation only from explicit `★ VALIDAR + ZIPRAF` action |
| light | none | presence/inventory only in current product path |
| proximity | none | presence/inventory only in current product path |
| step counter / step detector / physical-activity recognition | **not implemented** | `ACTIVITY_RECOGNITION` is not declared and must not be requested |
| sensor sampling above platform 200 Hz limit | **not implemented** | `HIGH_SAMPLING_RATE_SENSORS` is not declared; current probes request `SENSOR_DELAY_GAME` |
| camera | **not implemented** | no `CAMERA` permission |
| precise/coarse location | **not implemented** | no location permission |
| body sensors | **not implemented** | no `BODY_SENSORS` permission |
| Bluetooth scan/connect | **not implemented in this evidence path** | no Bluetooth runtime permission |
| RF transmit/modem control | **not implemented** | no RF-control permission or API path |

## Corrected inconsistency

V2 documented an optional `ACTIVITY_RECOGNITION` path from the SYS panel. Source inspection showed that the implemented probes use `TYPE_ACCELEROMETER` and `TYPE_MAGNETIC_FIELD`; no `TYPE_STEP_COUNTER`, `TYPE_STEP_DETECTOR` or physical-activity classifier exists in the current source.

Therefore the previous activity permission increased permission surface without an implemented feature that required it. V3 removes that permission from the manifest and removes the runtime request path.

## Explicit sensor evidence action

`★ VALIDAR + ZIPRAF` is the explicit application action that initiates the bounded μ∆ observations. Before registration, the UI status states that accelerometer and magnetometer will be observed for a limited window.

Current bounds from source:

- each probe duration is bounded to `500..5000 ms`;
- current caller requests `2200 ms`;
- sample storage is capped at `4096` events per probe;
- listener request is `SENSOR_DELAY_GAME`;
- observations are typed `OBSERVED_UNPROMOTED` and do not promote device-fault or causal claims.

## Fail-closed gate

`ci/sensor-permission-gate.sh` is bound into `:app:rafaeliaJavaGate` and fails before APK work if the current profile regresses by:

- declaring any permission outside `RECORD_AUDIO` and `ACCESS_NETWORK_STATE`;
- restoring `ACTIVITY_RECOGNITION` or its runtime request path;
- adding step/activity sensor APIs without a reviewed successor contract;
- declaring `HIGH_SAMPLING_RATE_SENSORS`;
- using `SENSOR_DELAY_FASTEST`;
- losing the explicit sensor-evidence disclosure or embedded permission contract.

This is a source/CI gate. It is **not** physical proof of OEM runtime behavior.

## External Android references — scope only

The Android documentation describes `ACTIVITY_RECOGNITION` for physical-activity/step use on Android 10+, and documents the sensor-event rate limit for target 31+ unless `HIGH_SAMPLING_RATE_SENSORS` is declared. These references inform the design boundary; they are not certification or an external audit of this application.

- https://developer.android.com/about/versions/10/privacy/changes#physical-activity-recognition
- https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview

## Evidence boundary

```text
MANIFEST_MINIMIZED = source-observable
RUNTIME_REQUEST_PATH_REMOVED = source-observable
CI_GATE_PASS = pending exact-head execution until CI completes
ANDROID10_PHYSICAL_PERMISSION_FLOW = NOT_RUN
OEM_SENSOR_RATE_BEHAVIOR = NOT_RUN
EXTERNAL_STANDARD_AUDIT = NOT_AUDITED
claim_allowed = false
```
