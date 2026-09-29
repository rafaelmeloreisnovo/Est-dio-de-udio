# DELTA4.4 — Adaptive Low UI / Gap Reconciliation CI PASS

```text
parent = docs/receipts/DELTA4_4_ADAPTIVE_LOW_GAPS_2026-09-29.md
repository = rafaelmeloreisnovo/Est-dio-de-udio
pull_request = #9
validated_head = 55e230e60a34d4c7e3b64dd33e791cdada83edb7
workflow = Audio Studio PR Gate
run_id = 36532446575
run_number = 87
result = PASS
```

## Executed evidence

```text
HOST_DSP_SMOKE = PASS
HOST_METER_SMOKE = PASS
HOST_MANIFOLD_SMOKE = PASS
FREESTANDING_SOURCE_GATE = PASS
LOW_DEPENDENCY_BOUNDARY = PASS
ARMV7_ZERO_UNDEFINED = PASS
ARMV7_ABI_MANIFEST = PASS
AARCH64_ZERO_UNDEFINED = PASS
CI_PROVENANCE_EMBED = PASS
LOW_SHA256_KAT = PASS
GRADLE_SETUP = PASS
ASSEMBLE_DEBUG_APK = PASS
APK_ARTIFACT_UPLOAD = PASS
```

Artifact:

```text
name = rafaelia-audio-studio-debug
size_bytes = 79098
sha256 = 6b0b5bf955fc4a845998d672369e72bee998840c56bd6b3ea21d7cdd68458c09
```

## Low boundary observed by CI

```text
JAVA_THIRD_PARTY_DEPS = 0
ANDROIDX = 0
KOTLIN = 0
R8_SHRINK = 0
JNI_IMPLEMENTATION_FILES = 1
```

## What remains physical

```text
ADAPTIVE_UI_PORTRAIT = NOT_RUN
ADAPTIVE_UI_LANDSCAPE = NOT_RUN
AUDIO_INPUT_ENUMERATION_PHYSICAL = NOT_RUN
LAPEL_STEREO_CAPABILITY = DEVICE_DEPENDENT_NOT_RUN
INSTALLED_APK_EVIDENCE = NOT_RUN
```

## R3

```text
F_ok =
  adaptive console source
  + low dependency gate
  + ARMv7/AArch64 gates
  + LowSha256 KAT
  + APK PASS

F_gap =
  physical portrait/landscape
  + real input/lapel enumeration
  + installed APK evidence bundle

F_next =
  install artifact -> rotate portrait/landscape
  -> inspect REC/EDIT/CAL/SPEC/ROOM/VOICE/MASTER/EXPORT
  -> generate PROOF bundle
  -> compare installed APK hash
```
