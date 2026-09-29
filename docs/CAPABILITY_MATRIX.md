# Capability Matrix

| Capability | Source | CI/build | Physical | Claim state |
|---|---|---|---|---|
| PCM capture | implemented | build-gated | device-dependent | bounded |
| DSP/meter freestanding cores | implemented | ARMv7/AArch64 gated | N/A | bounded |
| ZRF session container | implemented | build-gated | available | bounded |
| CFR relative capture | implemented | build-gated | requires run | physical result NOT_RUN until CFR exists |
| absolute SPL | schema only | N/A | reference required | PENDING_PHYSICAL_REFERENCE |
| IR/deconvolution | partial primitives | pending integration | NOT_RUN | PENDING |
| RT metrics | UI/contract route | not complete | NOT_RUN | PENDING |
| room correction | FIR primitive + route | not complete | NOT_RUN | PENDING |
| voice acoustic features | implemented reference | gated | input-dependent | bounded |
| pitch-preserving autotune | not complete | NOT_RUN | NOT_RUN | PENDING |
| accelerometer μ∆ vibration | implemented platform probe | build-gated | generated on device | OBSERVED_UNPROMOTED |
| hardware fault diagnosis | not implemented | N/A | reference/model absent | NOT_CLAIMED |
| installation evidence bundle | implemented | build-gated | generated on device | bounded |

Update this matrix whenever a receipt materially promotes or demotes a state.

| adaptive studio console | implemented | build-gated | portrait/landscape physical NOT_RUN | bounded source |
| audio input/lapel enumeration | implemented runtime query | build-gated | device-dependent | OBSERVED_UNPROMOTED |
| graphics capability query | implemented runtime query | build-gated | device-dependent | OBSERVED_UNPROMOTED |
| Java third-party dependencies | none declared | CI gated | N/A | 0 |
| R8/shrink path | disabled | CI gated | N/A | 0 |


| RAW PCM export | implemented | build-gated | physical NOT_RUN | bounded source |
| WAV PCM16 authorial export | implemented | build-gated | physical NOT_RUN | bounded source |
| runtime codec inventory | implemented | build-gated | device-dependent | OBSERVED_UNPROMOTED |
| MP3 decode | platform capability query/import path | build-gated | device-dependent | runtime-dependent |
| MP3 encode | not implemented | N/A | NOT_RUN | NOT_IMPLEMENTED |
| Opus encode | Android platform edge | build-gated | device-dependent | bounded platform path |
| RAC1 lossless PCM16 core | implemented | host/ARM gated | UI export not wired | IMPLEMENTED_UNTESTED |
| magnetometer μ∆ | implemented | build-gated | physical NOT_RUN | OBSERVED_UNPROMOTED when run |
| passive connectivity metadata | implemented | build-gated | physical NOT_RUN | PLATFORM_OBSERVATION_ONLY |
| RF transmit/control | not implemented | N/A | N/A | PROHIBITED_BY_PROJECT_BOUNDARY |
| ZRF CODE/PROV/RAC1 chunk ids | registered | source-gated | serialization partial | IMPLEMENTED_PARTIAL |


| Node.js application runtime | absent | CI-gated | N/A | 0 |
| npm/yarn/pnpm dependency graph | absent | CI-gated | N/A | 0 |
| authorial CI orchestration | implemented | pending execution | N/A | IMPLEMENTED_UNTESTED |
| checkout/setup Java/setup Android/setup Gradle JS Actions | removed from canonical gate | pending execution | N/A | SOURCE_REMOVED |
| external JavaScript Actions | none in canonical workflow | pending CI | N/A | 0 |
| GitHub CLI release transport | implemented platform edge | pending CI | N/A | PLATFORM_EDGE |
| installed APK signing certificate evidence | implemented | build-gated | device-dependent | bounded |
| real authorial signed release | workflow implemented | requires real secrets/vars | NOT_RUN | TOKEN_VAZIO_UNTIL_CONFIGURED |
| component-origin manifest | implemented | pending CI | embedded + post-build | bounded |
| SYS permissions/origin/signature workspace | implemented | pending CI | physical NOT_RUN | bounded source |
