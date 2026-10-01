# ZRF / CFR FORMAT V1

Copyright (c) 2026 Rafael Melo Reis
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**State:** `PROJECT_LOCAL_FORMAT_V1 / IMPLEMENTED / DELTA5_REMOTE_CI_PENDING`

ZRF and CFR are project-local container identifiers. They are not external standards.

## 1. Intent

- **ZRF**: session container for source audio plus optional wave/matrix/analysis chunks.
- **CFR**: calibration/response container for measured calibration, transfer, impulse-response and correction data.

The names are local contracts. Their semantics are versioned here rather than inferred from similarly named formats.

## 2. Main header — 40 bytes

| Offset | Bytes | Field |
|---:|---:|---|
| 0 | 4 | magic: `ZRF1` or `CFR1` |
| 4 | 1 | version |
| 5 | 1 | kind |
| 6 | 1 | channels |
| 7 | 1 | matrix dimension |
| 8 | 4 | sample rate LE |
| 12 | 2 | wave count LE |
| 14 | 2 | flags LE |
| 16 | 4 | chunk count LE |
| 20 | 4 | payload bytes LE |
| 24 | 4 | block samples LE |
| 28 | 4 | header bytes (=40) |
| 32 | 4 | FNV-1a-32 over bytes 0..31 |
| 36 | 4 | reserved |

All integers are bounded and little-endian.

## 3. Chunk header — 16 bytes

| Offset | Bytes | Field |
|---:|---:|---|
| 0 | 4 | fourcc/type LE |
| 4 | 4 | flags |
| 8 | 4 | payload bytes |
| 12 | 4 | item count |

Registered v1 chunk types:

```text
PCM   raw PCM source
WAVE  oscillator/wave descriptors
MATR  matrix/vector state
CAL   calibration record
IR    impulse response
SPEC  spectral analysis
PHON  phonetic/voice feature record
ROOM  room profile/correction state
RCPT  evidence/receipt record
CODE  codebook/schema metadata
PROV  provenance/origin/version metadata
RAC1  experimental RAC1-coded PCM payload
```

Unknown required chunks must fail closed. Unknown optional chunks may be skipped only after their bounded length is validated.

## 4. Source preservation

ZRF must not replace the source-of-record silently. The current implementation creates a ZRF sidecar while preserving the raw PCM file used by the mastering pipeline.

## 5. Block profiles

V1 recognizes working policies:

```text
128
512
4096
```

No performance claim follows from the chosen size.

## 6. Matrices and waves

`matrix_dim` is limited to 0..16 in V1.

The wave bank currently supports at most 16 Q15 sine voices. Future WAVE chunks must carry explicit phase, phase increment, gain, channel routing and sample-rate context.

A matrix chunk must declare fixed-point scale and shape; a matrix with no schema is invalid.

## 7. CFR calibration evidence

CFR may carry:

- device/input route identity;
- sample rate/channel map;
- digital noise floor;
- latency;
- relative frequency response;
- phase/coherence;
- channel mismatch;
- impulse response;
- correction filter;
- physical reference metadata when absolute SPL is claimed.

Without an acoustic reference, absolute SPL fields are `PENDING_PHYSICAL_REFERENCE`; no numeric SPL value is fabricated.

## 8. Integrity

The v1 FNV checksum is an accidental-corruption guard, not cryptographic authenticity.

A stronger receipt may independently attach SHA-256/BLAKE3 or another declared digest to the complete artifact.

```text
checksum != authenticity
hash != authorship
```


## 9. CFR relative calibration profile v1

An analyzed relative-capture session writes six chunks in this order:

```text
WAVE flags=0
CAL  flags=RELATIVE
PCM  flags=REFERENCE
PCM  flags=RESPONSE
SPEC flags=RELATIVE
ROOM flags=RELATIVE
```

### WAVE payload — 32 bytes

| Offset | Bytes | Field |
|---:|---:|---|
| 0 | 4 | sweep start phase-step Q32 |
| 4 | 4 | per-frame sweep ratio Q31 |
| 8 | 4 | excitation gain Q15 |
| 12 | 4 | reference/excitation frame count |
| 16 | 4 | pre-silence frames |
| 20 | 4 | post-silence frames |
| 24 | 4 | maximum latency-search lag |
| 28 | 4 | deterministic sync frame count |

The reference PCM contains:

```text
sync -> zero guard -> exponential sweep
```

The speaker path additionally has pre/post silence; those are not duplicated in the reference PCM.

### CAL payload — 48 bytes

| Offset | Bytes | Field |
|---:|---:|---|
| 0 | 4 | CAL payload version (=1) |
| 4 | 4 | state flags; bit0 = RELATIVE |
| 8 | 4 | best sync lag in samples |
| 12 | 4 | captured response frames |
| 16 | 8 | signed correlation; sign may indicate global path polarity at sync |
| 24 | 8 | sync reference energy |
| 32 | 8 | aligned response energy |
| 40 | 4 | sample rate |
| 44 | 4 | channels |

This is evidence of a relative speaker-room-microphone-ADC path. It does not establish absolute sound pressure.

```text
RELATIVE_CAPTURE != ABSOLUTE_SPL
SYNC_LAG != IMPULSE_RESPONSE
SWEEP_CAPTURE != RT60
```


### Polarity note

The alignment search ranks candidates by absolute correlation while preserving the signed value.

```text
positive correlation -> same sync polarity
negative correlation -> inverted sync polarity
SIGNED_SYNC_CORRELATION != FULL_PHASE_RESPONSE
```


## 10. Multimodal/codebook extension

V1 reserves three additional chunk identifiers:

```text
CODE -> versioned semantic/schema codebook
PROV -> provenance coordinates
RAC1 -> experimental lossless PCM16-coded payload
```

Registration does not require every ZRF to contain them.

A RAC1 payload does not replace the immutable source by itself. If RAC1 is used as a derived representation, source preservation/provenance rules still apply.

```text
CODEBOOK != CONTENT
HASH != AUTHORSHIP
RAC1_ROUNDTRIP != SUPERIOR_COMPRESSION
```


## 10. SPEC relative sweep profile — 344 bytes

```text
u32 version = 1
u32 kind = 1              # equal-time windows over exponential sweep
u32 band_count = 16
u32 valid_bands
u32 analyzed_frames
u32 power_ratio_fraction_bits = 20
repeat 16 times:
  u64 reference_energy
  u64 response_energy
  u32 power_ratio_q20
```

This is a relative energy profile. Equal time on an exponential sweep approximates logarithmic frequency windows; it is not a calibrated FFT/STFT or deconvolved impulse response.

## 11. ROOM relative decay profile — 72 bytes

```text
u32 version = 1
u32 kind = 1              # backward-integrated relative decay
u32 analyzed_frames
u32 noise_tail_frames
u64 raw_energy
u64 noise_energy_per_sample
u64 corrected_initial_energy
i32 t5_frames
i32 t10_frames
i32 t25_frames
i32 t35_frames
i32 edt60_frames
i32 t20_rt60_frames
i32 t30_rt60_frames
u32 flags                   # EDT=1, T20=2, T30=4
```

A negative threshold/estimate means insufficient dynamic range. These values remain relative and do not assert ISO 3382 conformance.
