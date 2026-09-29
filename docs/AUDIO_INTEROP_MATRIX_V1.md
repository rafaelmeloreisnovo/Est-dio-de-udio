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
| RAW PCM16 | yes | yes | Rafaelia platform edge | IMPLEMENTED |
| WAV PCM16 | platform import where supported | yes | authorial 44-byte RIFF/WAVE writer | IMPLEMENTED_UNTESTED |
| Ogg/Opus | platform decode where available | yes | Android MediaCodec edge | IMPLEMENTED; runtime capability matters |
| MP3 | platform decode where available | no canonical encoder | Android codec edge | DECODER_CAPABILITY_RUNTIME_QUERY |
| FLAC | platform/device dependent | platform/device dependent | Android codec edge | RUNTIME_QUERY_ONLY |
| ZRF | yes/project-local | yes | Rafaelia container | IMPLEMENTED |
| CFR | yes/project-local | yes | Rafaelia calibration container | IMPLEMENTED_PARTIAL |
| RAC1 | core encode/decode | core encode/decode | Rafaelia freestanding core | IMPLEMENTED_UNTESTED_CORE |

## Rule

External codecs remain at the interoperability edge. They do not define the internal DSP representation.

Android runtime codec enumeration is evidence of codecs exposed by that device. It is not evidence that Rafaelia authored those codecs.

## MP3

The project does not claim a Rafaelia MP3 encoder. MP3 export remains `NOT_IMPLEMENTED` unless a lawful, compatible implementation is deliberately added and gated.

## Opus

The current Android target can use the platform Opus path where the device reports support. This remains distinct from RAC1.

## RAC1

RAC1 is experimental lossless PCM16 coding. Its current claim is only exact round-trip when tests pass.

```text
SUPERIOR_TO_MP3 = TOKEN_VAZIO
SUPERIOR_TO_OPUS = TOKEN_VAZIO
```
