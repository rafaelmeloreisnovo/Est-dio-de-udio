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

Relative software gate:
- known source/response alignment;
- 16-band relative sweep profile validated by deterministic host vectors;
- relative decay estimator validated by deterministic host vectors.

Relative physical evidence:
- repeated CFR captures on the target device/room;
- raw reference/response preserved;
- repeatability and noise/dynamic-range recorded.

Absolute:
- traceable or otherwise documented acoustic reference;
- calibration method and uncertainty;
- physical repeatability.

## No-go

Release notes must not convert `PENDING`, `NOT_RUN` or `TOKEN_VAZIO` into marketing claims.


## Baseline evidence before Delta 5

`main@1a1b00bb93b0988224d8b24085a82029d1228c45` passed GitHub run `36545296255`, including host smokes, freestanding gate, ARMv7/AArch64 gates, APK assembly, binary-origin receipt and live prerelease publication. Delta 5 must obtain its own CI result before promotion.
