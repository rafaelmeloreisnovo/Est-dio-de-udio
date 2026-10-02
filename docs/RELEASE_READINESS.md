# Release Readiness

## Current exact software baseline

```text
SOURCE_SHA=469eb83b0d23286190c306b17c3fd378118dadd4
CI_RUN_ID=37014682335
CI_RUN_NUMBER=39
CI_CONCLUSION=SUCCESS
REPRO_BUILD_A_SHA256=1656df3ed0aed0038162400fc9734b4856a465539a513593172edc53c32f1c43
REPRO_BUILD_B_SHA256=1656df3ed0aed0038162400fc9734b4856a465539a513593172edc53c32f1c43
APK_REPRODUCIBLE_SAME_ENV=PASS
REPRO_SCOPE=SAME_SOURCE_SAME_PINNED_CI_ENVIRONMENT
INDEPENDENT_REPRODUCTION=NOT_CLAIMED
APK_SHA256=1656df3ed0aed0038162400fc9734b4856a465539a513593172edc53c32f1c43
APK_PACKAGE_ID=io.rafaelia.audiostudio
APK_MIN_SDK=29
APK_TARGET_SDK=35
APK_ABIS=armeabi-v7a,arm64-v8a
INSTALLABLE_STATIC=PASS
INSTALLED_PHYSICAL=NOT_RUN
SIGNING_MODE=DEBUG_NONAUTHORIAL
EXPECTED_CERT_SHA256=TOKEN_VAZIO
EXTERNAL_STANDARD_AUDIT=NOT_AUDITED
CLAIM_ALLOWED=false
```

This baseline supersedes older baseline references for **current software state only**. Older physical receipts remain valid historical evidence for their own exact SHA; they are not current-head evidence.

## Required for a testable APK release

- exact source SHA checkout PASS;
- quality/assurance consistency gates PASS;
- host DSP/meter/manifold smokes PASS where wired;
- pre-Android freestanding source/runtime gate PASS;
- ARMv7 zero-undefined + ABI PASS;
- AArch64 zero-undefined PASS;
- writable persistent native symbol count remains zero in the gated freestanding scope;
- local shadow diagnostics remain enforced;
- final Android ELF boundary explicitly classified rather than mislabeled as true freestanding;
- debug/release assembly PASS for the requested route;
- same-source same-pinned-environment exact-byte reproducibility gate PASS;
- static APK integrity/signature/zipalign/installability PASS;
- artifact digest recorded;
- capability matrix reconciled to the exact source/run.

## Native boundary rule

The current gate distinguishes two scopes:

```text
PRE_ANDROID_LINK_CORE_OBJECTS = freestanding gate scope
FINAL_ANDROID_SO = Android platform-linked edge
```

For the current baseline the final `.so` files declare Android platform libraries and therefore are not claimed as true freestanding. This does not regress the separately gated freestanding core-object scope.

## Required before claiming physical capability

- exact target Android version/device recorded;
- APK installed is byte-identical to the intended CI APK SHA-256;
- installed package/source SHA/CI run/signing certificate recorded;
- relevant hardware action executed;
- raw artifact preserved;
- receipt records environment and outcome;
- new ZIPRAF generated from that exact execution;
- internal ZIPRAF SHA256SUMS verified;
- raw evidence embedded in ZIPRAF is byte-identical to separately exported raw evidence;
- hash-only artifact references must remain distinguished from bytes embedded in the ZIPRAF.

Current exact-head physical state:

```text
INSTALLED_PHYSICAL=NOT_RUN
CURRENT_HEAD_PHYSICAL_ZIPRAF=TOKEN_VAZIO
```

Tracked by issue #29.

## Required before acoustic calibration claims

### Relative software gate

- known source/response alignment;
- 16-band relative sweep profile validated by deterministic host vectors;
- relative decay estimator validated by deterministic host vectors.

### Relative physical evidence

- repeated CFR captures on the same exact software/device/room configuration;
- raw reference/response preserved;
- sample rate, orientation, thermal/battery context and capture conditions recorded;
- repeatability plus noise/dynamic-range recorded;
- N, median, mean, dispersion and min/max reported before promotion.

### Absolute

- traceable or otherwise documented acoustic reference;
- calibration method;
- measurement uncertainty;
- physical repeatability.

Until then:

```text
absolute_spl=PENDING_PHYSICAL_REFERENCE
```

## Required before authenticity / authorial signing claims

- `AUTHORIAL_ANDROID_APKSIGNER` route executed;
- signing identity configured from real protected provider state;
- expected certificate SHA-256 concrete, not `TOKEN_VAZIO`;
- produced APK certificate observed;
- expected and observed certificate digests match;
- signed APK digest recorded and bound to exact source/run.

Current state:

```text
SIGNING_MODE=DEBUG_NONAUTHORIAL
AUTHORIAL_SIGNING=TOKEN_VAZIO
```

## Reproducibility boundary

The current CI proves one narrower property:

```text
same exact source
+ same pinned CI environment
+ clean rebuild
+ byte-identical APK SHA-256
= APK_REPRODUCIBLE_SAME_ENV=PASS
```

That result is useful but is not independent reproduction. Before claiming independent reproducibility:

- build the same exact source SHA in a separately controlled environment;
- capture toolchain/environment receipt independently;
- compare produced artifact digests;
- classify any mismatch before promotion.

Current state:

```text
APK_REPRODUCIBLE_SAME_ENV=PASS
REPRO_SCOPE=SAME_SOURCE_SAME_PINNED_CI_ENVIRONMENT
INDEPENDENT_REPRODUCTION=NOT_CLAIMED
```

## Provider promotion enforcement

Project CI gates do not by themselves prove provider-side branch/ruleset enforcement.

Required provider receipt:

- `main` protected or equivalent ruleset active;
- exact-head canonical workflow required before merge;
- force-push/deletion policy read back;
- promotion route documented.

Until that readback exists, do not translate `CI=PASS` into `PROVIDER_ENFORCEMENT=PASS`.

## No-go

Release notes, UI copy, README text and ZIPRAF claims must not convert any of these into marketing or scientific claims:

```text
PENDING
NOT_RUN
IMPLEMENTED_UNTESTED
TOKEN_VAZIO
NOT_AUDITED
OBSERVED_UNPROMOTED
NOT_CLAIMED
```

The governing invariant remains:

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
REPRODUCIBLE_SAME_ENV != INDEPENDENT_REPRODUCTION
```
