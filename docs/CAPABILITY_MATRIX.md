# Capability Matrix

This matrix separates software implementation/build evidence from physical execution and claims. A newer receipt may promote a cell only within the scope it actually executed.

The exact SHA/run/hash below is an **immutable executed baseline**, not a floating assertion about the current Git ref. A documentation-only successor may advance `main`; the newest exact CI receipt and issue #29 remain authoritative for the next physical-install target.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
EMBEDDED_BYTES != REFERENCED_HASH
REPRODUCIBLE_SAME_ENV != INDEPENDENT_REPRODUCTION
```

## Executed software baseline

- source: `main@469eb83b0d23286190c306b17c3fd378118dadd4`
- canonical CI: run `37014682335` / #39 = `SUCCESS`
- debug APK SHA-256: `1656df3ed0aed0038162400fc9734b4856a465539a513593172edc53c32f1c43`
- same pinned CI environment rebuild A/B: identical SHA-256 = `PASS`
- independent reproduction: `NOT_CLAIMED`
- physical installation of this baseline APK: `NOT_RUN`
- signing mode for this receipt: `DEBUG_NONAUTHORIAL`
- provider `main` enforcement at readback: `TOKEN_VAZIO_UNRESOLVED` (`protected=false`)
- external standards audit: `NOT_AUDITED`
- `claim_allowed=false`

## Audio / measurement

| Capability | Source | CI/build | Physical | Claim state |
|---|---|---|---|---|
| PCM capture | implemented | canonical software gate PASS | exact-head device run required | bounded source; physical execution NOT_RUN |
| DSP/meter freestanding cores | implemented | ARMv7/AArch64 pre-Android-link objects zero-undefined PASS; no writable persistent symbols | N/A | bounded core scope |
| final Android `librafaelia_audio.so` | implemented Android/JNI edge | ABI build PASS; allowed `NEEDED=libc.so,libm.so,libdl.so`; dynamic undefined count 0 | N/A | `ANDROID_PLATFORM_LINKED`; not true freestanding final ELF |
| ZRF session container | implemented | parser mutation/fuzz gate PASS | exact-head materialization required | bounded software scope |
| CFR relative capture | implemented | parser/build gated | exact-head CFR run required | physical result NOT_RUN until exact-head CFR exists |
| absolute SPL | schema/path only | no absolute-reference gate can pass without reference | reference required | `PENDING_PHYSICAL_REFERENCE` |
| relative 16-band sweep profile | implemented fixed-point core | deterministic host/vector evidence exists from prior development; baseline canonical gate SUCCESS but no feature-specific promotion line emitted in run #39 | exact-head CFR required | bounded relative path; no absolute claim |
| relative EDT/T20/T30 estimator | implemented fixed-point core | deterministic host/vector evidence exists from prior development; baseline canonical gate SUCCESS but no feature-specific promotion line emitted in run #39 | sufficient exact-head measured decay required | non-standard relative estimate; standards-conformant RT NOT_CLAIMED |
| IR/deconvolution | FIR/measurement primitives | inverse-sweep IR path not closed by baseline receipt | NOT_RUN | PENDING |
| standards-conformant RT metrics | relative estimator exists | official-vector/protocol gate not closed | NOT_RUN | NOT_CLAIMED |
| room correction | FIR primitive + route | automatic fitting/application not closed | NOT_RUN | PENDING |
| voice acoustic features | implemented reference | canonical build gate PASS | input/device-dependent | bounded source |
| pitch-preserving autotune | not complete | NOT_RUN | NOT_RUN | PENDING |
| hardware fault diagnosis | not implemented | N/A | reference/model absent | NOT_CLAIMED |

## Sensors / device evidence

| Capability | Source | CI/build | Physical | Claim state |
|---|---|---|---|---|
| accelerometer μ∆ vibration | implemented platform probe | sensor permission/access + assurance smoke PASS | exact-head run required | no observation claim without execution |
| magnetometer μ∆ | implemented platform probe | unavailable-state semantics PASS | exact-head run required; older reference device reported hardware absent | unavailable must remain `TOKEN_VAZIO_SENSOR_UNAVAILABLE_*`; no current observation claim |
| all platform-reported physical sensor inventory | `Sensor.TYPE_ALL` runtime inventory + optional feature descriptors | access/permission contract PASS | device-dependent; exact-head run required | platform observation only when executed |
| passive connectivity metadata | implemented | build-gated | exact-head run required | `PLATFORM_OBSERVATION_ONLY` when executed |
| RF transmit/control | not implemented | N/A | N/A | `PROHIBITED_BY_PROJECT_BOUNDARY` |
| installation evidence bundle | implemented | consistency/build gated | exact-head generation required | bounded only after exact APK/device evidence |
| SYS permissions/origin/signature workspace | implemented | baseline canonical gate PASS | exact-head physical execution required | bounded source/software scope |

## Formats / interoperability

| Capability | Source | CI/build | Physical | Claim state |
|---|---|---|---|---|
| RAW PCM export | implemented | build-gated | exact-head physical NOT_RUN | bounded source |
| WAV PCM16 authorial export | implemented | build-gated | exact-head physical NOT_RUN | bounded source |
| runtime codec inventory | implemented | build-gated | device-dependent | `OBSERVED_UNPROMOTED` only after execution |
| MP3 decode | platform query/import path | build-gated | device-dependent | runtime-dependent |
| MP3 encode | not implemented | N/A | NOT_RUN | `NOT_IMPLEMENTED` |
| Opus encode | Android platform edge | build-gated | device-dependent | bounded platform path |
| RAC1 lossless PCM16 core | implemented | host/ARM/parser gate coverage present | UI export path not proven by current physical receipt | source implemented; physical/UI promotion withheld |
| ZRF CODE/PROV/RAC1 chunk ids | registered | source/parser gated | serialization path partial | `IMPLEMENTED_PARTIAL` |

## UI / runtime boundary

| Capability | Source | CI/build | Physical | Claim state |
|---|---|---|---|---|
| adaptive studio console | implemented | build-gated | portrait/landscape exact-head NOT_RUN | bounded source |
| audio input/lapel enumeration | implemented runtime query | build-gated | device-dependent | observation only when executed |
| graphics capability query | implemented runtime query | build-gated | device-dependent | observation only when executed |
| Java third-party dependencies | none declared | baseline CI reports `JAVA_THIRD_PARTY_DEPS=0` | N/A | 0 declared third-party Java deps |
| AndroidX | absent | baseline CI reports `ANDROIDX=0` | N/A | 0 |
| Kotlin app runtime | absent | baseline CI reports `KOTLIN=0` | N/A | 0 |
| R8/shrink path | disabled | baseline CI reports `R8_SHRINK=0` | N/A | 0 |
| Node.js application runtime | absent | baseline CI reports `NODE_APP_RUNTIME=0` | N/A | 0 |
| external JavaScript Actions | none in canonical workflow | baseline CI topology gate PASS | N/A | `NONE` |

## Assurance / delivery

| Capability | Source | CI/build | Physical/provider | Claim state |
|---|---|---|---|---|
| exact source checkout | implemented | run #39 `RAFAELIA_CHECKOUT=PASS` | N/A | bounded to exact SHA |
| parser fuzz / sanitizer gate | implemented | corpus 8192; ASAN/UBSAN PASS | N/A | executed software scope |
| assurance consistency gate | implemented | baseline `PASS_EXECUTED_SCOPE`; custody split delta requires its own exact-head PASS before promotion | N/A | fail-closed |
| ZIPRAF embedded/reference custody split | implemented in hardening delta | explicit gate requires `embedded + referenced` counters and custody classes | exact-head ZIPRAF still required | software semantics only until physical execution |
| unavailable-sensor semantics | implemented | `SENSOR_UNAVAILABLE_SEMANTICS=PASS` | exact-head device run still required | no false promotion |
| binary-origin receipt | implemented | run #39 PASS | N/A | bounded byte-origin metadata |
| same-environment APK reproducibility | implemented | build A = build B = `1656df3e...` | same pinned CI environment only | `PASS`; independent reproduction NOT_CLAIMED |
| independent reproduction | procedure gap remains | same-env test is insufficient | second controlled environment/device required | `NOT_CLAIMED` |
| installed APK signing certificate evidence | implemented | build-gated | exact-head device-dependent | bounded after physical evidence |
| real authorial signed release | workflow implemented + provider enforcement gate | route not executed in run #39 | requires provider enforcement + real vars/secrets/certificate | `TOKEN_VAZIO_UNTIL_CONFIGURED_AND_EXECUTED` |
| provider main protection/ruleset | issue #28 | application CI cannot supply provider policy | readback `protected=false` | `TOKEN_VAZIO_UNRESOLVED` |
| exact-head physical ZIPRAF | implemented generator | software consistency gate PASS | newest exact APK/device run required | `TOKEN_VAZIO_CURRENT_HEAD_PHYSICAL` |
| external standards audit | references may guide design | not an internal CI property | independent audit absent | `NOT_AUDITED` |

Update this matrix only from a receipt that identifies its exact source, execution scope and evidence boundary. Do not rewrite an immutable receipt when `main` advances; append/promote from the newer exact receipt instead.
