# AUDIO FORMAT PACK V1 · 2026-10-02

## Intent

Increase useful audio interoperability without regressing the low-dependency/freestanding boundary.

## Added scope

```text
RAW PCM16
WAV PCM16
AIFF PCM16
AU/SND PCM16
CAF LPCM16
```

The format pack is uncompressed PCM16. No third-party codec library is introduced.

## Ownership boundary

- `app/src/main/cpp/rfa_audio_format_core.c/.h`: strict freestanding header/endian core;
- `app/src/main/java/io/rafaelia/audiostudio/AudioInteropWriter.java`: provider-free Android file edge;
- `native/tests/audio_format_core_smoke.c`: byte-layout/endian vectors for the C core;
- `native/tests/AudioInteropWriterSmoke.java`: byte-layout/endian vectors for the Java writer;
- `ci/audio-format-core-gate.sh`: freestanding compile + zero-undefined + smoke gate;
- `native/abi/public_symbols_v1.txt`: micromodule ABI registration;
- `ci/rafaelia-pipeline.sh`: ARMv7/AArch64 architecture/ABI inclusion.

## Claims boundary

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
FREESTANDING_C_CORE != ANDROID_FILE_IO_EDGE
PUBLIC_SPECIFICATION != PUBLIC_DOMAIN_PROOF
```

No MP3 encoder is added. Ogg/Opus remains an Android platform codec edge. A project-authored FLAC encoder is not part of this delta.

## Promotion gates

Before merge the exact PR head must pass:

1. format-core freestanding compile;
2. host C format vectors;
3. provider-free Java format vectors;
4. Java/Android compile;
5. ARMv7 zero undefined + ABI manifest equality;
6. AArch64 zero undefined;
7. deterministic APK build;
8. exact-byte same-environment reproduction;
9. static APK installability;
10. binary origin receipt.

Physical export/import checks on the target handset remain `NOT_RUN` until the exact promoted APK is installed and exercised.

```text
EXTERNAL_STANDARD_AUDIT=NOT_AUDITED
PHYSICAL_FORMAT_INTEROP=NOT_RUN
claim_allowed=false
```
