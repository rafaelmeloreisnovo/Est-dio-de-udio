# START HERE — Rafaelia Audio Studio

## 1. What this is

Rafaelia Audio Studio is an Android audio workstation and measurement research platform with a narrow Android edge and authorial fixed-point freestanding C cores.

## 2. Choose your route

| I want to… | Read |
|---|---|
| use the app | `USER_GUIDE.md` |
| understand features | `CAPABILITY_MATRIX.md` |
| integrate/develop | `DEVELOPER_GUIDE.md` |
| understand architecture | `AUDIO_MANIFOLD_CONTRACT_V1.md` |
| inspect relative CFR analysis | `CFR_RELATIVE_ANALYSIS_V1.md` |
| understand adaptive noise/reference gating | `NLMS_REFERENCE_GATE_V1.md` |
| validate an installation | `INSTALLATION_VALIDATION.md` |
| test hardware | `HARDWARE_TEST_PROTOCOL.md` |
| understand μ∆ vibration | `MICRO_DELTA_VIBRATION_V1.md` |
| audit claims/evidence | `VERIFICATION_AND_EVIDENCE.md` |
| publish/share the product | `PUBLICATION_INDEX.md` |
| prepare a release | `RELEASE_READINESS.md` |
| understand terminology | `GLOSSARY.md` |

## 3. Current epistemic rule

```text
implementation is not execution
execution is not evidence
evidence is not every possible claim
```

The evidence button inside the app creates an installation/hardware/development bundle, including installed APK SHA-256 and a bounded accelerometer μ∆ observation.


## Low/adaptive implementation route

- `ADAPTIVE_STUDIO_UI_V1.md` — responsive studio console.
- `LOW_LEVEL_BOUNDARY_V1.md` — freestanding core vs irreducible Android shell.
- `TOKEN_GAP_RECONCILIATION_V1.md` — when to resolve, classify or retain an empty token.
- `NLMS_REFERENCE_GATE_V1.md` — adaptive cancellation reference/VAD boundary and evidence gates.


## System / origin / signing

- `AUTHORIAL_BINARY_ORIGIN_V1.md` — project/platform/toolchain/generated binary boundary.
- `SENSOR_PERMISSION_MATRIX_V2.md` — minimal permission policy.
- `SIGNED_RELEASE_V1.md` — real signing, certificate verification and public receipt.
