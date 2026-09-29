# DELTA4.2 CFR RELATIVE CAPTURE — CI PASS RECEIPT

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

```text
parent = docs/receipts/DELTA4_2_CFR_RELATIVE_CAPTURE_2026-09-29.md
repository = rafaelmeloreisnovo/Est-dio-de-udio
pull_request = #7
validated_head = 4c079bc0464bdc5ceef1d98c11f3f2fd92bcd6cc
workflow = Audio Studio PR Gate
run_id = 36528277873
run_number = 83
result = PASS
```

## Executed evidence

```text
SDK_NDK_CMAKE = PASS
HOST_DSP_SMOKE = PASS
HOST_METER_SMOKE = PASS
HOST_MANIFOLD_SMOKE = PASS
FREESTANDING_SOURCE_GATE = PASS
ARMV7_ZERO_UNDEFINED = PASS
ARMV7_ABI_MANIFEST = PASS
AARCH64_ZERO_UNDEFINED = PASS
GRADLE_SETUP = PASS
ASSEMBLE_DEBUG_APK = PASS
APK_ARTIFACT_UPLOAD = PASS
```

## Reconciled failures

Run #80 failed at ARMv7 zero-undefined because
`rfa_measure_core.o` referenced the public symbol
`rfa_wave_sine_q15`.

Correction:

```text
shared private header = rfa_sine_q15.h
lookup = static inline
wave public ABI = preserved through wrapper
measure cross-object undefined = removed
```

The CI workflow itself had previously been structurally corrupted by a split
ABI grep and duplicated AArch64/Gradle blocks. Both `pr-android.yml` and
`android.yml` were rebuilt canonically before run #83.

## Claim gate

```text
CFR_RELATIVE_IMPLEMENTATION = PASS_CI
APK_BUILD = PASS
PHYSICAL_ANDROID10 = NOT_RUN
SPEAKER_MIC_CAPTURE = NOT_RUN
CFR_PHYSICAL_ARTIFACT = TOKEN_VAZIO
ABSOLUTE_SPL = TOKEN_VAZIO
IR_DECONVOLUTION = PENDING
RT60 = PENDING
ROOM_CORRECTION = PENDING
```

CI proves the source/build/ABI path, not acoustic behavior on a real device.

## R3

```text
F_ok =
  CFR relative capture source
  + deterministic sync/sweep
  + polarity-aware latency alignment
  + reproducible CFR framing
  + ARMv7/AArch64 zero-undefined
  + APK build PASS

F_gap =
  physical Android 10 execution
  + actual CFR capture
  + acoustic reference for SPL
  + IR/transfer/coherence/THD derivation

F_next =
  install current APK on ARM32 Android 10
  -> run CAL relative capture at safe speaker level
  -> preserve first CFR
  -> inspect raw reference/response and receipt
```
