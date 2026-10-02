# Audio Interoperability Matrix V1

## Canonical internal route

```text
RAW SOURCE
-> PCM16 internal audio
-> DSP / matrices / metadata
-> ZRF/CFR
-> interoperability edge
```

| Format/path | Read | Write | Implementation authority | Current state |
|---|---|---|---|---|
| RAW PCM16 | yes | yes | authorial provider-free Java edge + freestanding C format core | CI_GATE_BOUND |
| WAV PCM16 | platform import where supported | yes | authorial RIFF/WAVE PCM16 writer + freestanding C header core | CI_GATE_BOUND |
| AIFF PCM16 | platform import where supported | yes | authorial FORM/COMM/SSND writer + freestanding C header core | CI_GATE_BOUND |
| AU/SND PCM16 | platform import where supported | yes | authorial `.snd` linear PCM16 writer + freestanding C header core | CI_GATE_BOUND |
| CAF LPCM16 | platform import where supported | yes | authorial `caff/desc/data` LPCM writer + freestanding C header core | CI_GATE_BOUND |
| Ogg/Opus | platform decode where available | yes | Android MediaCodec edge | IMPLEMENTED; runtime capability matters |
| MP3 | platform decode where available | no canonical encoder | Android codec edge | DECODER_CAPABILITY_RUNTIME_QUERY |
| FLAC | platform/device dependent | platform/device dependent | Android codec edge | RUNTIME_QUERY_ONLY |
| ZRF | yes/project-local | yes | Rafaelia container | IMPLEMENTED |
| CFR | yes/project-local | yes | Rafaelia calibration container | IMPLEMENTED_PARTIAL |
| RAC1 | core encode/decode | core encode/decode | Rafaelia freestanding core | IMPLEMENTED_CORE_GATED |

## Freestanding boundary

`rfa_audio_format_core.c/.h` is the strict freestanding serialization core. It is compiled with `-nostdinc -ffreestanding -fno-builtin`, participates in the ARMv7/AArch64 zero-undefined ABI gate, uses no libc/libm/heap/filesystem/network/thread API and owns only byte-layout/endian primitives.

The Android/Java edge remains hosted by definition: it opens files/MediaStore streams and writes bytes. It does not turn Android I/O into a freestanding claim.

```text
FREESTANDING_C_CORE != ANDROID_FILE_IO_EDGE
PROVIDER_FREE_WRITER != PUBLIC_DOMAIN_CLAIM
```

## Format-scope rule

The V1 expansion deliberately covers uncompressed PCM16 containers first: RAW, WAV, AIFF, AU/SND and CAF/LPCM. That increases interoperability without adding an external codec library or hiding compression behind platform/native providers.

Specification availability and public documentation are not treated as proof that a format specification is legally in the public domain. The implementation in this repository is project-authored; no third-party codec source is embedded by this format pack.

## External codecs

External codecs remain at the interoperability edge. They do not define the internal DSP representation.

Android runtime codec enumeration is evidence of codecs exposed by that device. It is not evidence that Rafaelia authored those codecs.

### MP3

The project does not claim a Rafaelia MP3 encoder. MP3 export remains `NOT_IMPLEMENTED` unless a lawful, compatible implementation is deliberately added and gated.

### Opus

The current Android target can use the platform Opus path where the device reports support. This remains distinct from the freestanding PCM-container core and from RAC1.

### FLAC

A project-authored FLAC encoder is not part of this V1 delta. FLAC remains `RUNTIME_QUERY_ONLY` until a separately reviewed implementation, conformance vectors and performance gates exist.

## RAC1

RAC1 is experimental lossless PCM16 coding. Its claim remains scoped to exact round-trip where the relevant tests execute and pass.

```text
SUPERIOR_TO_MP3 = TOKEN_VAZIO
SUPERIOR_TO_OPUS = TOKEN_VAZIO
EXTERNAL_STANDARD_AUDIT = NOT_AUDITED
CLAIM_ALLOWED = false
```
