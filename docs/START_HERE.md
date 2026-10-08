# START HERE — Rafaelia Audio Studio

> **P0 CI efficiency / queue (2026-10-08):** [Why 31m of wait had ~2m18s of build time, Draft quick gate, ready-for-review full gate, stale-run cancellation, rollback](ROADMAPS_IA_HUMANOS/CI_FAST_QUEUE_HOTFIX_20261008.md). Quick PASS cannot establish binary PASS or physical-device receipt.

> **Java low-level / ZIPRAF (2026-10-08):** [Authorial ZIP32 serializer, CRC32, bounded byte path and interoperability gates](ROADMAPS_IA_HUMANOS/LOWLEVEL_JAVA_ZIPRAF_FREESTANDING_DELTA_20261008.md). This source delta does not replace required APK CI, provider enforcement or physical-device receipts.

> **Latest P0 operations (2026-10-08):** [Provider enforcement + audio session + freestanding hardening](ROADMAPS_IA_HUMANOS/P0_PROVIDER_AUDIO_FREESTANDING_HOTFIX_20261008.md). This is a change-route memo; its CI and physical gates remain separate from historical executed receipts below.

## 1. What this is

Rafaelia Audio Studio is an Android audio workstation and measurement research platform with a narrow Android edge and authorial fixed-point freestanding C cores.

Repository relationship observed on 2026-10-04: `fork=false`. Project-authored source may use the repository's explicit `LicenseRef-RAFCODE-Research-Commercial-0.1`, while Android, Java, Gradle, AGP, NDK, CMake, Clang, Git, GitHub and other external/toolchain material remain separate provenance classes. Repository control does not reclassify them as project-authored.

## 2. Choose your route

| I want to… | Read |
|---|---|
| use the app | `USER_GUIDE.md` |
| understand features | `CAPABILITY_MATRIX.md` |
| integrate/develop | `DEVELOPER_GUIDE.md` |
| understand architecture | `AUDIO_MANIFOLD_CONTRACT_V1.md` |
| inspect relative CFR analysis | `CFR_RELATIVE_ANALYSIS_V1.md` |
| understand adaptive noise/reference gating | `NLMS_REFERENCE_GATE_V1.md` |
| audit parser fuzz / ARM runtime regressions | `PARSER_FUZZ_ARM_RUNTIME_GATE_V1.md` |
| validate an installation | `INSTALLATION_VALIDATION.md` |
| test hardware | `HARDWARE_TEST_PROTOCOL.md` |
| understand μ∆ vibration | `MICRO_DELTA_VIBRATION_V1.md` |
| audit claims/evidence | `VERIFICATION_AND_EVIDENCE.md` |
| inspect Android permission/sensor boundaries | `SENSOR_PERMISSION_MATRIX_V3.md` |
| inspect authorship vs generated/platform bytes | `AUTHORIAL_BINARY_ORIGIN_V1.md` |
| inspect repository license terms | `../LICENSE_RESEARCH_COMMERCIAL.md` |
| inspect agent/change authority | `../AGENTS.md` |
| publish/share the product | `PUBLICATION_INDEX.md` |
| prepare a release | `RELEASE_READINESS.md` |
| understand terminology | `GLOSSARY.md` |

## 3. Current state — bounded readback

Observed current `main` on 2026-10-04:

```text
MAIN_HEAD                    = 8ca8ec9f0b8aa58ee3b22a5c950b3750c36b7eb8
LATEST_MERGED_PR             = #46
PR_EXACT_HEAD                = f06bc2340f2431b70835281f25b3cdabe2ca9f7f
PR_EXACT_HEAD_CI             = 37158616393 SUCCESS
POST_MERGE_CI                = 37158856337 SUCCESS
APK_BYTES_FOR_PHYSICAL_TEST  = NOT_PROVISIONED
PHYSICAL_ANDROID             = NOT_RUN
ZIPRAF                       = TOKEN_VAZIO
claim_allowed_end_to_end     = false
```

The source/CI closure proves only the executed software gates. It does not prove installation on the Moto E7, microphone/speaker behavior, acoustic conformity, physical sensor behavior, ZIPRAF custody, or end-to-end product quality.

Before acting on a SHA/run above, re-read GitHub. This section is a dated reconstruction coordinate, not an evergreen status oracle.

## 4. Current epistemic rule

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
CI_PASS != PHYSICAL_PASS
```

The evidence button inside the app creates an installation/hardware/development bundle. The explicit `★ VALIDAR + ZIPRAF` action also starts bounded accelerometer and magnetometer μ∆ observations; these sensors do not require an additional Android runtime permission in the current profile. `ACTIVITY_RECOGNITION` is not declared because step/activity classification is not implemented.

## 5. Lowest-friction reconstruction order

```text
AGENTS.md
-> README.md
-> docs/START_HERE.md
-> relevant contract/source only
-> exact CI/source receipt
-> physical receipt when the claim crosses into hardware
```

Do not load every document by default. Expand only for a missing source, contradiction, unresolved authority, missing evidence, or explicit request.

For licensing/authorship questions, use this order:

```text
exact path
-> file header/SPDX if present
-> LICENSE_RESEARCH_COMMERCIAL.md for project-authored material
-> AUTHORIAL_BINARY_ORIGIN_V1.md for project/platform/toolchain/generated boundaries
-> applicable third-party/platform terms
-> TOKEN_VAZIO if lineage is still unresolved
```

The GitHub license detector currently reports `NOASSERTION` because the repository uses a project-authored nonstandard license file. That detector state is not a substitute for reading `LICENSE_RESEARCH_COMMERCIAL.md`, and the custom license must not be projected onto third-party/platform material.

## Low/adaptive implementation route

- `ADAPTIVE_STUDIO_UI_V1.md` — responsive studio console.
- `LOW_LEVEL_BOUNDARY_V1.md` — freestanding core vs irreducible Android shell.
- `TOKEN_GAP_RECONCILIATION_V1.md` — when to resolve, classify or retain an empty token.
- `NLMS_REFERENCE_GATE_V1.md` — adaptive cancellation reference/VAD boundary and evidence gates.
- `PARSER_FUZZ_ARM_RUNTIME_GATE_V1.md` — malformed-input sanitizer gate and ARM runtime-helper regression control.

## System / origin / signing

- `AUTHORIAL_BINARY_ORIGIN_V1.md` — project/platform/toolchain/generated binary boundary.
- `SENSOR_PERMISSION_MATRIX_V3.md` — current minimal permission policy and fail-closed sensor gate.
- `SENSOR_PERMISSION_MATRIX_V2.md` — retained historical predecessor; not current operational guidance.
- `SIGNED_RELEASE_V1.md` — real signing, certificate verification and public receipt.

## F_next

The smallest evidence-producing next step is unchanged by this documentation refresh: obtain the exact APK bytes and SHA-256 for current `main`, install the same artifact on the intended physical Android target, capture the bounded physical receipt, and only then update physical/ZIPRAF claims.

R3=<F_ok: current source/CI, license route and provenance route are explicit, F_gap: APK bytes + physical Android + ZIPRAF remain NOT_PROVISIONED/NOT_RUN/TOKEN_VAZIO, F_next: exact-same-artifact physical acquisition and receipt without promoting CI across evidence classes>
