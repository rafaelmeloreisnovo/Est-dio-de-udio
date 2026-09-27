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
