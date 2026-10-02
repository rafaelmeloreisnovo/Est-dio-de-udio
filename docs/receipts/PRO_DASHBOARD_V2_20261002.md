# PRO_DASHBOARD_V2 — implementation receipt — 2026-10-02

## Intent
Raise the highest-value UX surface without expanding scientific claims or adding unvalidated audio metrics.

## Source
- base: `main@69f2121d249fd3dad5097fbc3f7f733faab4f1a4`
- implementation branch: `ui/pro-dashboard-v2`
- UI commit: `190a981d44ba22d9fbd13553b7c012f679630b41`

## Materialized delta
`StudioWorkspaceView` was reduced from a flat nine-screen surface to five professional sections:

- `DASH`
- `MEASURE`
- `SESSION`
- `EVIDENCE`
- `SYSTEM`

All pre-existing executable callbacks remain wired:

- record
- stop + master
- play master
- relative calibration
- evidence / ZIPRAF generation
- optional motion sensor access

The new dashboard prioritizes only runtime state already available to the app: input source, peak percentage, RMS percentage, clipping count, captured samples/time, waveform, 16-band relative spectrum, relative calibration bands/profile, room-analysis state, container state, provenance state, signature state and sensor/platform state.

## Explicit non-expansion
Not added/promoted:

- LUFS measurement
- THD/THD+N measurement
- absolute SPL
- calibrated FFT/STFT claim
- hardware fault diagnosis
- authorial signature PASS
- independent reproduction PASS
- external standards audit PASS

The UI keeps `TOKEN_VAZIO`, `PENDING`, `NOT_RUN` and unavailable states visible rather than replacing them with optimistic labels.

## Sensor permission semantics
The SYSTEM section explicitly distinguishes sensor availability from Android runtime permissions:

- accelerometer: platform sensor; no invented runtime permission;
- magnetometer/light/proximity: hardware/runtime availability state;
- microphone: `RECORD_AUDIO` remains explicit;
- optional motion access remains a separate explicit action.

## Gate state at write time
- SOURCE_DELTA=PASS
- CALLBACK_PRESERVATION=PASS_SOURCE_REVIEW
- JAVA_COMPILE=NOT_RUN
- APK_BUILD=NOT_RUN
- DEVICE_INSTALL=NOT_RUN
- PHYSICAL_UI_REVIEW=NOT_RUN
- CLAIM_ALLOWED=false

`IMPLEMENTED_UNTESTED != PASS`.
