# Evidence ledger — Delta 1

## Fontes normativas e técnicas

1. RFC 6716 — Definition of the Opus Audio Codec.
2. RFC 7845 — Ogg Encapsulation for the Opus Audio Codec.
3. Android 10 media documentation — Opus encoding introduced at API 29.
4. Android MediaMuxer.OutputFormat — MUXER_OUTPUT_OGG introduced at API 29.
5. Android supported media formats — Opus encoding/decoding with Ogg.
6. Android MediaRecorder/AudioSource — UNPROCESSED when supported; VOICE_RECOGNITION as fallback.
7. ITU-R BS.1770-5 — loudness and true-peak measurement.
8. EBU R 128 v5 / Tech 3341 — loudness normalization and EBU Mode metering.

## Gates

| Capability | State | Evidence rule |
|---|---|---|
| DSP source exists | PASS | files committed |
| No heap/libm in dsp_core source | PASS | source inspection |
| ARMv7 object undefined symbols | PASS | cross-compile ELF32 ARM EABI5; nm -u = 0 after removing __aeabi_uidivmod |
| Host DSP smoke | PASS | clang host smoke exit 0 |
| APK assembleDebug | PENDING_CI | GitHub Actions |
| Android 10 physical install | NOT_RUN | device receipt |
| Real WhatsApp Ogg/Opus import | NOT_RUN | sample receipt |
| Ogg/Opus export opens/plays | NOT_RUN | player + duration check |
| No clipping regression | NOT_RUN | measured PCM/output |
| BS.1770-5 LUFS | PENDING | algorithm + conformance vectors |
| True peak | PENDING | oversampled implementation + tests |

claim_allowed=false

CI_BASE_WORKFLOW_INSTALLED=true


## Receipt 2026-09-27 — kernel gate
- host_smoke=PASS
- armv7_compile=PASS
- armv7_object=ELF32_ARM_EABI5
- armv7_undefined_symbols=0
- hotfix=removed modulo helper __aeabi_uidivmod
- apk_build=ROUTE_STATE_BLOCKED
- github_actions_runs_observed=0
- physical_audio_test=NOT_RUN


# Evidence ledger — Delta 2

## Implemented

| Capability | State | Evidence rule |
|---|---|---|
| First-run wizard | IMPLEMENTED_UNTESTED | APK/UI test |
| Mic capability report | IMPLEMENTED_UNTESTED | physical device |
| Raw source preservation | IMPLEMENTED_UNTESTED | filesystem receipt |
| Disable AGC/NS/AEC best-effort | IMPLEMENTED_UNTESTED | session/device receipt |
| Narration editor + teleprompter | IMPLEMENTED_UNTESTED | APK/UI test |
| PCM playback | IMPLEMENTED_UNTESTED | physical playback |
| 16-band Goertzel spectrum | IMPLEMENTED_UNTESTED | synthetic vectors |
| BS.1770 K-weighting 48 kHz | IMPLEMENTED_UNTESTED | ITU/EBU vectors |
| 400 ms / 75% gating | IMPLEMENTED_UNTESTED | unit/conformance vectors |
| -70 absolute / -10 relative gate | IMPLEMENTED_UNTESTED | conformance vectors |
| Annex-2 4x true-peak FIR | IMPLEMENTED_UNTESTED | true-peak vectors |
| Fixed-point gated normalization | IMPLEMENTED_UNTESTED | before/after measurement |
| -1 dBTP gain ceiling | IMPLEMENTED_UNTESTED | physical + vectors |
| ARMv7 zero undefined Delta2 | PENDING_CI | llvm-nm -u |
| AArch64 zero undefined Delta2 | PENDING_CI | llvm-nm -u |
| APK assembleDebug Delta2 | PENDING_CI | GitHub Actions |

## Provenance

- ITU-R BS.1770-5 (11/2023), Annex 1: K-weighting and gated loudness.
- ITU-R BS.1770-5 (11/2023), Annex 2: true-peak 4x interpolation FIR.
- EBU R128 v5 (11/2023): -23 LUFS target and true-peak descriptor.
- EBU Tech 3341: EBU Mode metering.
- Android platform docs: AudioRecord, AudioTrack, UNPROCESSED, AGC/NS/AEC.

## Claim gate

BS1770_CONFORMANCE=PENDING
EBU_R128_CONFORMANCE=PENDING
TRUE_PEAK_CONFORMANCE=PENDING
APK_BUILD=PENDING_CI
PHYSICAL_AUDIO=NOT_RUN
claim_allowed=false

## μWRITE

kind=delta2_audio_professional
source=feature/pro-audio-delta2-narration-metering
parent=Delta1 merged main
summary=wizard+narration+metering+truepeak+spectrum+normalization+playback
evidence=source_commits+CI_pending
gap=conformance_vectors|CI|physical_audio|whatsapp_roundtrip
next=run_CI_and_fix_until_zero_undefined_plus_APK
