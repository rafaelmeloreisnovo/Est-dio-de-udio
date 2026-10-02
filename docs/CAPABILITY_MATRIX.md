# Capability Matrix

Current software baseline: `main@def87c720b14ab30ff39371637530d172223da6c`, GitHub Actions run `37013439446` = `SUCCESS`.

Epistemic boundary:

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
IMPLEMENTED_UNTESTED != PASS
INSTALLABLE_STATIC != INSTALLED_PHYSICAL
TOKEN_VAZIO != 0
```

| Capability | Source | CI/build | Physical | Claim state |
|---|---|---|---|---|
| PCM capture | implemented | current main gate PASS | current-head device run required | bounded |
| DSP/meter freestanding cores | implemented | pre-Android ARMv7/AArch64 zero-undefined gates PASS | N/A | bounded to core-object scope |
| final Android native `.so` boundary | implemented | current main gate PASS | packaged in debug APK | `ANDROID_PLATFORM_LINKED`; not claimed true freestanding |
| writable persistent native state | hardened | ARMv7/AArch64 writable persistent symbols = 0 | N/A | PASS_EXECUTED_SCOPE |
| local shadow diagnostics | implemented | `-Wshadow,-Werror` gate PASS | N/A | bounded compile gate |
| ZRF session container | implemented | current main gate PASS | current-head run required | bounded |
| CFR relative capture | implemented | current main gate PASS | current-head CFR required | physical result `NOT_RUN` until exact-head capture exists |
| absolute SPL | schema only | N/A | traceable reference required | `PENDING_PHYSICAL_REFERENCE` |
| relative 16-band sweep profile | implemented fixed-point core | current main gate PASS | current-head CFR run required | bounded relative path |
| relative EDT/T20/T30 estimator | implemented fixed-point core | current main gate PASS | sufficient measured decay required | non-standard relative estimate |
| IR/deconvolution | FIR/measurement primitives | inverse-sweep IR not integrated | `NOT_RUN` | `PENDING` |
| standards-conformant RT metrics | relative estimator exists | official vectors/protocol still pending | `NOT_RUN` | `NOT_CLAIMED` |
| room correction | FIR primitive + route | automatic fitting/application incomplete | `NOT_RUN` | `PENDING` |
| voice acoustic features | implemented reference | current main gate PASS | input-dependent | bounded |
| pitch-preserving autotune | not complete | `NOT_RUN` | `NOT_RUN` | `PENDING` |
| accelerometer μ∆ vibration | implemented platform probe | Java/sensor gate PASS | exact-current-head device execution `NOT_RUN` | no current-head physical promotion |
| magnetometer μ∆ | implemented availability-aware probe | sensor semantics gate PASS | exact-current-head device execution `NOT_RUN` | unavailable hardware remains `TOKEN_VAZIO_SENSOR_UNAVAILABLE` |
| physical sensor inventory | implemented platform query | sensor gate PASS | device-dependent | observation only |
| hardware fault diagnosis | not implemented | N/A | reference/model absent | `NOT_CLAIMED` |
| installation evidence bundle | implemented | build-gated | exact-current-head bundle required | bounded |
| ZIPRAF assurance container | implemented | consistency/model gates PASS | new exact-current-head ZIPRAF required | integrity/custody only; not authenticity/certification |
| passive connectivity metadata | implemented | build-gated | device-dependent | `PLATFORM_OBSERVATION_ONLY` |
| RF transmit/control | not implemented | N/A | N/A | `PROHIBITED_BY_PROJECT_BOUNDARY` |
| adaptive studio console | implemented | build-gated | portrait/landscape current-head physical `NOT_RUN` | bounded source |
| audio input/lapel enumeration | implemented runtime query | build-gated | device-dependent | `OBSERVED_UNPROMOTED` only after execution |
| graphics capability query | implemented runtime query | build-gated | device-dependent | `OBSERVED_UNPROMOTED` only after execution |
| RAW PCM export | implemented | build-gated | current-head physical `NOT_RUN` | bounded source |
| WAV PCM16 authorial export | implemented | build-gated | current-head physical `NOT_RUN` | bounded source |
| runtime codec inventory | implemented | build-gated | device-dependent | runtime-dependent |
| MP3 decode | platform capability query/import path | build-gated | device-dependent | platform-dependent |
| MP3 encode | not implemented | N/A | `NOT_RUN` | `NOT_IMPLEMENTED` |
| Opus encode | Android platform edge | build-gated | device-dependent | bounded platform path |
| RAC1 lossless PCM16 core | implemented | host/ARM gates PASS | UI export path remains separate | bounded to executed gate scope |
| ZRF CODE/PROV/RAC1 chunk ids | registered | source-gated | serialization partial | `IMPLEMENTED_PARTIAL` |
| Java third-party dependencies | none declared | current main gate reports `JAVA_THIRD_PARTY_DEPS=0` | N/A | 0 |
| AndroidX | absent | current main gate reports `ANDROIDX=0` | N/A | 0 |
| Kotlin | absent | current main gate reports `KOTLIN=0` | N/A | 0 |
| Node.js application runtime | absent | CI-gated | N/A | 0 |
| npm/yarn/pnpm dependency graph | absent | CI-gated | N/A | 0 |
| R8/shrink path | disabled | current main gate reports `R8_SHRINK=0` | N/A | 0 |
| canonical CI orchestration | implemented | run `37013439446` PASS | N/A | PASS_EXECUTED_SCOPE |
| exact checkout binding | implemented | `RAFAELIA_CHECKOUT=PASS` on run `37013439446` | N/A | exact source bound |
| parser fuzz / ASAN / UBSAN | implemented | corpus 8192; ASAN/UBSAN PASS on current main | N/A | PASS_EXECUTED_SCOPE |
| static APK installability | implemented | current main PASS | not equivalent to install | `INSTALLABLE_STATIC=PASS` |
| physical installation of current head | procedure defined | CI cannot prove it | `NOT_RUN` | tracked by issue #29 |
| installed APK signing certificate evidence | implemented | build-gated | device-dependent | bounded |
| real authorial signed release | workflow implemented | requires real secrets/vars | `NOT_RUN` | `TOKEN_VAZIO_UNTIL_CONFIGURED` |
| component-origin manifest | implemented | current main evidence gate PASS | post-build/physical binding separate | bounded |
| provider-side main enforcement | workflow gates exist | repository readback still not a software gate | provider action required | tracked separately; do not infer enforcement from CI |
| external standards conformity | references may guide gates | no external audit receipt | `NOT_AUDITED` | `NOT_AUDITED` |

## Current exact APK coordinate

For `main@def87c720b14ab30ff39371637530d172223da6c`, run `37013439446` recorded:

```text
APK_SHA256=09703c7564980f9aa1374a689a10fc3408ffd321488ecaab56726de416eb19ee
APK_PACKAGE_ID=io.rafaelia.audiostudio
APK_MIN_SDK=29
APK_TARGET_SDK=35
APK_ABIS=armeabi-v7a,arm64-v8a
INSTALLABLE_STATIC=PASS
INSTALLED_PHYSICAL=NOT_RUN
SIGNING_MODE=DEBUG_NONAUTHORIAL
EXPECTED_CERT_SHA256=TOKEN_VAZIO
```

Update this matrix only from an exact source/run/device receipt. A new `main` SHA invalidates the phrase "current-head physical" until that new SHA is executed physically.
