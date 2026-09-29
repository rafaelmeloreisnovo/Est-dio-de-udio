# AUDIO_MANIFOLD_CONTRACT_V1

Copyright (c) 2026 Rafael Melo Reis
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**State:** `ARCHITECTURE_IMPLEMENTED_PARTIAL + claim_allowed=false`  
**Execution target:** Android API 29+ with freestanding C micromodules behind a narrow platform edge.

## 1. Invariant

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
```

The studio is not a bag of effects. It is a typed audio state machine in which capture, time, frequency, voice, room, calibration, DSP and evidence remain separable.

## 2. Eight knowledge/execution levels

| Ω | Layer | Canonical observables | Current route |
|---|---|---|---|
| Ω1 | sample | PCM, peak, RMS, clipping | existing recorder/meter |
| Ω2 | spectral | bands, FFT/STFT candidates, phase, coherence | Goertzel-16 implemented; broader analysis pending |
| Ω3 | voice | f0, formants, voicing, phoneme/timing features | contract only |
| Ω4 | dynamics | EQ-N, gate, level, limiter, normalization | partial DSP rack |
| Ω5 | spatial | IR, convolution, room transfer, stereo/multimic | format/UI contract; measurement engine pending |
| Ω6 | causal | excitation -> transfer -> evidence | ZRF/CFR + calibration route |
| Ω7 | manifold | typed 7D/14-axis feature representations | experimental mapping only |
| Ω8 | governance | enable/bypass/A-B/gate/receipt/rollback | CI + state labels |

## 3. Workspaces

The platform surface exposes:

```text
REC | EDIT | CAL | SPEC | ROOM | VOICE | MASTER | EXPORT
```

Each workspace reads the same source state but owns distinct controls. The transport and original source are not silently rewritten.

## 4. Audio state

A future canonical frame may carry independent typed projections such as:

```text
sample/amplitude
time/sample_index
frequency/phase
loudness/peak
voice/f0/formant/voicing
spectral/coherence
room/IR/RT
calibration/reference
transform/history
confidence/evidence_state
```

Fields with different units are not directly added. A matrix/vector view requires explicit normalization and schema.

## 5. Micromodules

Current freestanding additions:

- `rfa_wave_core`: up to 16 deterministic Q15 sine voices using phase accumulators and a fixed quarter-wave table;
- `rfa_matrix_core`: caller-owned Q15 matrix up to 16×16;
- `rfa_block_core`: caller-owned ring buffer and block profiles 128/512/4096;
- `rfa_container_core`: bounded ZRF/CFR headers and typed chunks;
- `rfa_fir_core`: caller-owned FIR/convolution kernel for IR/reverb/correction;
- `rfa_time_core`: Q16 linear time-map/resampler; explicitly not pitch-preserving;
- `rfa_lms_core`: bounded adaptive LMS noise-cancellation reference;
- `rfa_biquad_core`: up to 16 Q30 biquad sections with explicit coefficients.

These modules have no heap, libc, libm, filesystem, network or threading dependency.

## 6. Block/cache policy

```text
128 -> short analysis/control block
512 -> default studio block
4096 -> bounded batch/render block
```

These sizes are policies, not claims about a hardware unit called “NIU”. The label NIU remains `TOKEN_VAZIO` until a concrete hardware/interface specification is identified.

The platform may later select a profile from measured latency/cache characteristics. A profile change must produce evidence rather than infer performance from the number alone.

## 7. Hardware acceleration boundary

CPU SIMD, GPU compute, DSP/NPU accelerators or other hardware may be used only through an explicit adapter.

```text
canonical scalar core
      |
      +-- optional SIMD adapter
      +-- optional GPU adapter
      +-- optional DSP/NPU adapter
```

The scalar core remains the reference. Accelerator output must pass equivalence vectors before promotion.

`GPU_AVAILABLE`, `NPU_AVAILABLE`, `NUMA_AVAILABLE` and similar capabilities are runtime observations, never assumptions.

## 8. ELF / DEX / NUMA

- ELF is an executable/object container and evidence surface for symbols/ABI.
- DEX is Android bytecode packaging and belongs to the platform shell.
- NUMA is meaningful only on hardware/OS exposing non-uniform memory topology.
- none of these concepts is treated as DSP mathematics.

The current Android phone target should normally assume `NUMA=TOKEN_VAZIO` unless actually detected.

## 9. Time and dubbing

The architecture distinguishes:

```text
pitch shift != time stretch != local time warp
```

A future local warp must use a monotonic mapping `t_out = W(t_in)` and preserve source provenance. Formant, pitch, unvoiced regions and timing confidence are independently gateable.

## 10. Parametric EQ

The target model is N bounded bands, each with an explicit type, center frequency, gain and Q. Automatic room/voice EQ must remain a candidate until fitted against a measured target with regularization and boost/Q limits.

## 11. Reverb / room

Three independent routes are planned:

- algorithmic multichannel feedback;
- convolution from measured impulse response;
- room correction from measured transfer response.

Room correction must not perform unconstrained inverse boosting of deep spatial nulls.

## 12. Calibration

Calibration states:

```text
CAL_DIGITAL      -> dBFS/peak/RMS/clipping
CAL_RELATIVE     -> raw speaker-room-mic capture + latency alignment implemented;
                    transfer/phase/coherence derivation remains staged
CAL_ABSOLUTE_SPL -> requires a physical acoustic reference
```

Without a physical reference:

```text
ABS_SPL = TOKEN_VAZIO
```

### 12.1 Relative physical capture

The first physical calibration route uses:

```text
pre-silence
-> deterministic sync
-> zero guard
-> fixed-point exponential sweep
-> post-silence
```

The microphone capture is stored in CFR together with the exact digital excitation.
Sync correlation estimates the discrete path latency. This does not yet constitute
an impulse response, calibrated transfer function, RT60, or absolute SPL measurement.

## 13. Phoneme / phonetics

A phonetic layer may derive measurable acoustic features from recorded speech:

- voicing;
- f0;
- spectral envelope/formants;
- onset/offset;
- duration;
- periodicity;
- confidence.

Phoneme labels are annotations/inferences over mechanical acoustic waves. They are not evidence of a quantum or neurological mechanism by themselves.

## 14. Poincaré / 7D / 14-axis bridge

Repository geometry may be reused as a feature-space representation only.

A 7D or 14-axis audio embedding must declare:

- exact feature vector;
- units/normalization;
- transform;
- inverse or reconstruction condition where claimed;
- loss metric;
- test vectors;
- falsifier.

```text
GEOMETRIC_EMBEDDING != PHYSICAL_SPACETIME_DIMENSION
```

## 15. Quantum and neuroscience boundary

Acoustic pressure waves, digital signals, neural responses and quantum mechanics occur at different descriptive scales.

The repository may contain experiments or hypotheses joining them, but the audio studio shall label such modules:

```text
EXPERIMENTAL
claim_allowed=false
```

until an explicit physical model, instrument, dataset, uncertainty analysis and independent reproduction exist.

## 16. Stop condition

A feature may be promoted only when:

```text
SOURCE
+ SPEC
+ IMPLEMENTATION
+ TEST
+ EXECUTION
+ RECEIPT
+ CLAIM_GATE
```

are individually resolved.
