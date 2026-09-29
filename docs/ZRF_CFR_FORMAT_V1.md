# ZRF / CFR FORMAT V1

Copyright (c) 2026 Rafael Melo Reis
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**State:** `PROJECT_LOCAL_FORMAT_V1 / IMPLEMENTED_UNTESTED`

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

Without an acoustic reference, absolute SPL fields remain `TOKEN_VAZIO`.

## 8. Integrity

The v1 FNV checksum is an accidental-corruption guard, not cryptographic authenticity.

A stronger receipt may independently attach SHA-256/BLAKE3 or another declared digest to the complete artifact.

```text
checksum != authenticity
hash != authorship
```
