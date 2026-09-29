# DELTA4.3 — Documentation / Evidence / μ∆ CI PASS Receipt

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

```text
parent = docs/receipts/DELTA4_3_DOCS_EVIDENCE_MUDELTA_2026-09-29.md
repository = rafaelmeloreisnovo/Est-dio-de-udio
pull_request = #8
validated_head = 214bca427b0d66ab0d58eba3199b4560490625ff
workflow = Audio Studio PR Gate
run_id = 36530761494
run_number = 85
result = PASS
```

## Executed evidence

```text
HOST_DSP_SMOKE = PASS
HOST_METER_SMOKE = PASS
HOST_MANIFOLD_SMOKE = PASS
FREESTANDING_SOURCE_GATE = PASS
ARMV7_ZERO_UNDEFINED = PASS
ARMV7_ABI_MANIFEST = PASS
AARCH64_ZERO_UNDEFINED = PASS
CI_PROVENANCE_ASSET_EMBED = PASS
GRADLE_SETUP = PASS
ASSEMBLE_DEBUG_APK = PASS
APK_ARTIFACT_UPLOAD = PASS
```

Artifact:

```text
name = rafaelia-audio-studio-debug
size_bytes = 81616
sha256 = cc11bf66d0ae64ae81d5f85fc37d5e8cd7b0f435003abd24c4e5c96a18c7f592
```

## What this validates

- scoped AGENTS/doc files do not break packaging;
- BuildConfig provenance fields compile;
- CI provenance asset is embedded before APK assembly;
- evidence bundle source compiles;
- installed-APK hashing path compiles;
- MediaStore export path compiles;
- accelerometer μ∆ probe compiles;
- prior audio/DSP ARM gates remain PASS.

## What remains physical

```text
EVIDENCE_BUTTON_PHYSICAL_ANDROID10 = NOT_RUN
INSTALLED_APK_HASH_ON_DEVICE = TOKEN_VAZIO
ACCELEROMETER_MUDELTA_CAPTURE = NOT_RUN
FIRST_EXPORTED_EVIDENCE_BUNDLE = TOKEN_VAZIO
HARDWARE_FAULT_DIAGNOSIS = NOT_CLAIMED
```

## R3

```text
F_ok =
  documentation family
  + scoped AGENTS
  + on-device evidence bundle implementation
  + CI provenance embedding
  + μ∆ accelerometer implementation
  + APK build PASS

F_gap =
  install on target Android 10
  + execute evidence button
  + preserve first physical μ∆ observation
  + verify installed APK digest against downloaded artifact bytes

F_next =
  install APK -> tap Gerar provas + teste μ∆
  -> preserve exported bundle
  -> compare installed_apk_sha256 with artifact APK digest
```
