# Release Readiness

## Required for a testable APK release

- host DSP/meter/manifold smokes PASS;
- freestanding source gate PASS;
- ARMv7 zero-undefined + ABI PASS;
- AArch64 zero-undefined PASS;
- assembleDebug/release PASS;
- artifact digest recorded;
- documentation capability matrix reconciled.

## Required before claiming physical capability

- target Android version/device recorded;
- installation evidence bundle generated;
- relevant hardware action executed;
- raw artifact preserved;
- receipt records environment and outcome.

## Required before acoustic calibration claims

Relative:
- repeated CFR captures;
- known source/response alignment;
- transfer derivation validated.

Absolute:
- traceable or otherwise documented acoustic reference;
- calibration method and uncertainty;
- physical repeatability.

## No-go

Release notes must not convert `PENDING`, `NOT_RUN` or `TOKEN_VAZIO` into marketing claims.
