# FREESTANDING_CORE_CONTRACT_V3

status: IMPLEMENTED_UNTESTED
claim_allowed: false
branch: feature/freestanding-core-v3-contexts
parent: main@058a3a5a8f79d1103a224e484a4797d51d151966

## SOURCE

Core:
- app/src/main/cpp/rfa_core_types.h
- app/src/main/cpp/dsp_core.h
- app/src/main/cpp/dsp_core.c
- app/src/main/cpp/meter_core.h
- app/src/main/cpp/meter_core.c

Adapter:
- app/src/main/cpp/jni_bridge.c

Evidence producers:
- native/tests/dsp_smoke.c
- native/tests/meter_smoke.c
- .github/workflows/pr-android.yml
- .github/workflows/android.yml

## CONTRACT

CORE must satisfy all simultaneously:

- no system headers;
- no hosted runtime calls;
- no heap;
- no mutable file-scope state;
- no undefined symbols in ARMv7 object;
- no undefined symbols in AArch64 object;
- bounded exported ABI;
- deterministic caller-owned state;
- no Android/JNI symbols;
- no filesystem/network/thread API.

## ABI V3

DSP exports only:
- dsp_apply_gain_q30
- dsp_state_process
- dsp_state_reset

Meter exports only:
- meter_spectrum16
- meter_state_gain_q30
- meter_state_push
- meter_state_relative_gate_block
- meter_state_reset
- meter_state_result

## EVIDENCE RULE

PASS requires a GitHub Actions run on this branch/PR in which:
- both host smoke tests pass;
- source contract gate passes;
- ARMv7 undefined-symbol gate passes;
- ARMv7 ABI allowlist passes;
- AArch64 undefined-symbol gate passes;
- assembleDebug passes;
- APK artifact is uploaded.

Until that run exists:
IMPLEMENTED_UNTESTED != PASS.
