# NLMS Reference Gate V1

## Purpose

Bound the smallest safe step from the existing adaptive LMS research kernel toward measured noise cancellation without inventing a reference channel or overwriting raw capture.

Canonical invariant:

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
IMPLEMENTED_UNTESTED != PASS
TOKEN_VAZIO != 0
```

## Current source boundary

The Android capture path is PCM16 mono at 48 kHz and preserves the raw primary microphone stream before mastering. That mono stream is **not** a valid independent adaptive-noise reference by itself.

Therefore:

```text
PRIMARY_SOURCE = Android mono microphone capture
REFERENCE_SOURCE = TOKEN_VAZIO
RAW_PCM_MUTATION = FORBIDDEN
NLMS_PIPELINE_WIRING = BLOCKED_BY_REFERENCE_SOURCE
```

No synthetic, duplicated or delayed copy of the primary microphone signal may be labeled as a measured noise reference.

## Core delta

`rfa_lms_core` retains the existing LMS API and adds a separate bounded fixed-point NLMS primitive:

- caller-owned Q15 coefficients and history;
- caller-provided `mu_q15`;
- caller-provided `epsilon_q30` regularizer;
- no libc/libm/heap/filesystem/thread dependency;
- no mutable file-scope state;
- `adapt=0` freezes coefficient updates while continuing estimation with the current filter;
- the normalization division is implemented inside the core to avoid introducing a hosted integer-division runtime dependency on ARMv7.

The adaptation gate is intentionally policy-neutral. A VAD may drive it later, but the NLMS primitive does not claim to identify speech.

## VAD boundary

`rfa_voice_core` already exposes bounded features (energy, zero crossings and periodic correlation), but a validated voice/activity decision threshold has not been established for the physical Android microphone path.

```text
VAD_FEATURES = IMPLEMENTED
VAD_DECISION_POLICY = TOKEN_VAZIO
VAD_PHYSICAL_VALIDATION = NOT_RUN
SPEAKER_IDENTIFICATION = OUT_OF_SCOPE
VOICE_EMBEDDING_STORAGE = NOT_IMPLEMENTED
```

A future VAD gate should freeze NLMS adaptation while desired speech is active when the chosen reference topology makes that necessary. Thresholds must come from a declared dataset or physical protocol, not an arbitrary constant hidden in the kernel.

## Deterministic host vector

`native/tests/manifold_smoke.c` now includes a synthetic primary/reference pair where correlated interference is known by construction. The vector checks:

1. NLMS convergence reduces the known correlated-error aggregate by at least 10x over the measured tail of the vector.
2. `adapt=0` leaves the learned coefficient unchanged.

This vector validates arithmetic/control behavior only. It is not evidence of Android acoustic noise reduction.

## Physical integration gate

Pipeline wiring is allowed only after all mandatory inputs are explicit:

```text
GATE_A_REFERENCE_SOURCE = TOKEN_VAZIO
GATE_B_REFERENCE_ALIGNMENT = TOKEN_VAZIO
GATE_C_VAD_POLICY = TOKEN_VAZIO
GATE_D_HOST_VECTOR = PENDING_CI
GATE_E_ARMV7_ZERO_UNDEFINED = PENDING_CI
GATE_F_AARCH64_ZERO_UNDEFINED = PENDING_CI
GATE_G_ANDROID10_PHYSICAL = NOT_RUN
GATE_H_ZRF_BEFORE_AFTER_RECEIPT = NOT_RUN
```

Candidate reference sources may include a physically distinct microphone channel, USB multichannel input, or another explicitly measured reference path. Selection is a hardware/topology decision and is not inferred here.

## Evidence/claim state

```text
NLMS_SOURCE = IMPLEMENTED_ON_FEATURE_BRANCH
NLMS_HOST_VECTOR = IMPLEMENTED_UNTESTED
NLMS_ARMV7 = NOT_RUN
NLMS_AARCH64 = NOT_RUN
NLMS_ANDROID10 = NOT_RUN
NOISE_REDUCTION_PHYSICAL = NOT_RUN
claim_allowed = false
```

After CI, only the gates actually executed may be promoted. Physical audio claims remain blocked until a real Android/reference capture is recorded and hashed in the evidence bundle/ZRF chain.

R3=<F_ok: bounded NLMS primitive + deterministic vector + adaptation freeze contract; F_gap: physical reference/VAD thresholds/Android run remain absent; F_next: CI exact-head -> choose measured reference topology -> physical ZRF before/after run>
