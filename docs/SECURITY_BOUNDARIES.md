# Security & Trust Boundaries

## Protected by design

- no backup of app private data;
- bounded custom container fields;
- raw/source preservation;
- narrow JNI bridge;
- freestanding core dependency gates;
- public C ABI manifest;
- installed APK SHA-256 in evidence bundle;
- Android permission surface limited to `RECORD_AUDIO` and `ACCESS_NETWORK_STATE` in the current profile;
- microphone permission requested only from an explicit audio action;
- accelerometer/magnetometer μ∆ observation starts only from the explicit `★ VALIDAR + ZIPRAF` action;
- current sensor probes use a bounded time/sample window and do not request `HIGH_SAMPLING_RATE_SENSORS`.

## Not guaranteed

- cryptographic authenticity of every external input;
- anti-tamper against a compromised OS;
- secure hardware attestation;
- privacy of files after the user shares them;
- scientific validity from hashes alone;
- identical OEM sensor scheduling or permission UI across physical devices until tested.

## Sensitive data

Audio recordings and hardware/sensor evidence can contain personal or device context. Share evidence bundles deliberately and minimize unrelated recordings.

The current evidence action can include bounded accelerometer/magnetometer measurements and a sensor capability inventory. This is application evidence collection, not a hidden Android runtime permission. `ACTIVITY_RECOGNITION` is not part of the current feature set because the app does not implement step counter/detector or physical-activity classification.

## Permission and sensor boundary

```text
RECORD_AUDIO = ASK_ON_EXPLICIT_AUDIO_ACTION
ACCESS_NETWORK_STATE = PASSIVE_PLATFORM_METADATA_ONLY
ACTIVITY_RECOGNITION = NOT_DECLARED_NO_STEP_ACTIVITY_FEATURE
HIGH_SAMPLING_RATE_SENSORS = NOT_DECLARED_CURRENT_PROFILE
ACCELEROMETER/MAGNETOMETER = NO_RUNTIME_PERMISSION_REQUIRED
SENSOR_MUDELTA = EXPLICIT_PROOF_ACTION
PHYSICAL_DEVICE_PERMISSION_FLOW = NOT_RUN
EXTERNAL_STANDARD_AUDIT = NOT_AUDITED
```

Security findings should be documented with source, reproduction conditions and evidence rather than speculative severity.

Operational permission details and the fail-closed gate are defined in `SENSOR_PERMISSION_MATRIX_V3.md`.
