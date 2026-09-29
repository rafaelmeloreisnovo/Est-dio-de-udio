# RAC1 — Rafaelia Audio Codec Experiment V1

**State:** `IMPLEMENTED_UNTESTED_CORE`

RAC1 is an experimental lossless PCM16 coding primitive. It is not presented as an MP3/Opus replacement.

## Input

- PCM signed 16-bit;
- 1 or 2 interleaved channels;
- frame count supplied by caller.

## Transform

For each channel independently:

```text
previous[0] = 0
delta[n] = sample[n] - previous[channel]
previous[channel] = sample[n]
zigzag(delta) -> unsigned integer
unsigned integer -> 7-bit varint
```

Delta magnitude for PCM16 fits within the bounded 3-byte varint used by V1.

## Decode

```text
varint -> zigzag inverse -> delta
sample = previous[channel] + delta
```

The decoder rejects truncation, malformed varints, impossible reconstructed PCM16 values and trailing encoded bytes.

## Required gate

```text
PCM input
-> RAC1 encode
-> RAC1 decode
-> exact sample comparison
```

The host manifold smoke contains this round-trip and a truncated-payload fail-closed vector.

## What V1 does not claim

- best compression ratio;
- perceptual superiority;
- lower bitrate than Opus/MP3;
- streaming resilience;
- error correction;
- cryptographic authenticity.

Those remain benchmark/evidence questions.

```text
SUPERIOR_TO_MP3_OPUS = TOKEN_VAZIO
```
