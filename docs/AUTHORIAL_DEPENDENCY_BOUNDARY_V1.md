# RAFAELIA Audio — Authorial Dependency Boundary V1

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: `LicenseRef-RAFCODE-Research-Commercial-0.1`

## Goal

Reduce app/runtime coupling and integration friction without pretending that Android SDK/NDK, Gradle, AGP, CMake, R8, JDK or platform ABIs are project-authored implementations.

```text
project algorithm
!= project control client
!= provider factory tool
!= platform ABI
!= runtime evidence
```

The reusable classification is maintained by RafPolimata. This repository remains the implementation authority for the audio app.

## Current dependency surface

| Surface | Current audio state | Boundary |
|---|---|---|
| C | authorial DSP/manifold/format cores | freestanding source/object gates before Android link |
| Java | authorial app/platform edge | Android platform/ART only |
| Kotlin | not used by current app | disabled; no reason to add while Java edge is sufficient |
| Python | local/CI validation only | stdlib-only control client; not app runtime |
| Gradle | project task contract + pinned external binary | factory/build only |
| AGP | pinned external Android plugin | factory/build only |
| SDK | API/build/package edge | platform + factory; not a third-party app library |
| NDK | ABI/toolchain edge | native Android factory; not freestanding proof |
| CMake | target graph/orchestration | factory/build only |
| JNI | one authorial bridge source | Android JNI ABI boundary |
| R8 | disabled | optional transform requires separate semantic-equivalence gate |
| assemble | artifact construction | never promoted to runtime or physical PASS |

## Current low-friction invariants

The existing quality pipeline already rejects:

- `androidx.*`, `kotlin.*` or `com.google.*` imports in the Java app edge;
- Gradle runtime/library dependency declarations;
- R8/resource shrinking enabled in the canonical path;
- more than one JNI implementation source;
- system headers, heap/libc/runtime calls and mutable file-scope state inside the freestanding C core scope.

The new local gate additionally binds exact tool identities and the RafPolimata control-contract pointer without network fetch.

## Authorial versus external

The preferred direction is not “rewrite every external factory tool.” The preferred direction is:

1. keep algorithms and data transformations project-owned where feasible;
2. keep the Android/platform edge narrow;
3. treat SDK/NDK/Gradle/AGP/CMake as declared factory tools;
4. pin identities and record transforms;
5. replace a provider only if the replacement truly reduces total coupling and can be implemented clean-room with provenance and tests;
6. never remove provider copyright/license notices from material that remains provider-derived.

## UI refactor in this delta

The app already renders its primary studio surface as a custom dark, platform-only view. The prior application shell declared an Android **Light** theme, which could make platform widgets/system chrome visually inconsistent.

This delta introduces `RafaeliaAudioTheme`, based only on the Android platform theme, and aligns:

- window background;
- status/navigation bars;
- accent color;
- light/dark system icon policy;
- no-title shell.

No AndroidX or Material Components dependency is added.

## Evidence ceiling

```text
source gate PASS
!= APK build PASS
!= install PASS
!= launch PASS
!= physical audio/sensor PASS
```

The exact-head CI decides the build gates. Physical install/audio/sensor/ZIPRAF evidence remains separate and must not be inferred from source structure.

## Rollback

All changes are branch-local and additive except the manifest theme pointer and Gradle task binding. Rollback is a normal PR close/revert; no stored audio, ABI layout or core algorithm is migrated by this change.

## R3

`F_ok` = dependency roles are explicit; the gate is no-network/fail-closed; UI shell matches the existing dark studio without a new UI framework.  
`F_gap` = exact-head CI and physical-device evidence are separate and may remain `TOKEN_VAZIO`.  
`F_next` = execute the existing canonical workflow on this branch and inspect the same-head receipts before any merge decision.
