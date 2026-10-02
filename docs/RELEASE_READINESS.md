# Release Readiness

Release readiness is evaluated per evidence class. A green software gate does not promote provider policy, physical execution, calibration, signing authority or external audit.

The exact receipt recorded below is an **immutable executed baseline**. A documentation-only merge may advance `main`; the newest exact CI receipt and issue #29 must be used for the next physical-install target rather than silently rewriting this historical evidence.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
IMPLEMENTED_UNTESTED != PASS
TOKEN_VAZIO != 0
EMBEDDED_BYTES != REFERENCED_HASH
REPRODUCIBLE_SAME_ENV != INDEPENDENT_REPRODUCTION
```

## Executed software baseline

Validated runtime-bearing baseline before the current custody delta:

- source: `main@469eb83b0d23286190c306b17c3fd378118dadd4`
- canonical GitHub Actions run: `37014682335` / #39 = `SUCCESS`
- debug APK SHA-256: `1656df3ed0aed0038162400fc9734b4856a465539a513593172edc53c32f1c43`
- package: `io.rafaelia.audiostudio`
- ABIs: `armeabi-v7a`, `arm64-v8a`
- static installability: `PASS`
- physical installation of this exact APK: `NOT_RUN`
- same pinned CI/toolchain rebuild A/B byte identity: `PASS`
- independent reproduction: `NOT_CLAIMED`
- signing mode: `DEBUG_NONAUTHORIAL`
- authorial signing certificate expectation: `TOKEN_VAZIO` for this debug receipt
- provider main protection: `TOKEN_VAZIO_UNRESOLVED`
- external standards audit: `NOT_AUDITED`
- `claim_allowed=false`

## Testable debug APK — software gate

The baseline canonical receipt supports the executed software scope below:

- exact detached checkout of the declared source SHA;
- Java compile + native build for both Android ABIs;
- host quality/smoke gates;
- parser mutation/fuzz corpus with ASAN/UBSAN PASS;
- ARMv7/AArch64 pre-Android-link freestanding objects with zero undefined symbols;
- public ABI manifest check on ARMv7;
- no writable persistent symbols in the freestanding object scope;
- `-Wshadow -Werror` diagnostic gate;
- final Android `.so` boundary classified separately as `ANDROID_PLATFORM_LINKED` with allowed `NEEDED=libc.so,libm.so,libdl.so`;
- assurance consistency and unavailable-sensor semantics gates;
- APK ZIP integrity/signature verification/zipalign/static installability;
- binary-origin receipt;
- clean second build in the same pinned CI environment with exact APK byte identity.

This does **not** imply a physical-device PASS or independent reproduction.

## ZIPRAF custody gate

The current hardening delta closes one narrower ambiguity without adding a product feature:

```text
materialized_count = embedded_artifact_count + referenced_artifact_count
```

Required semantics:

- raw evidence copied into `80_raw/evidence.txt` = `EMBEDDED`;
- installed APK hash = `REFERENCED_NOT_EMBEDDED`;
- concrete ZRF/CFR/mastered-PCM hashes = `REFERENCED_NOT_EMBEDDED` unless their bytes are explicitly added as ZIP entries;
- missing referenced artifacts remain typed `TOKEN_VAZIO_NOT_MATERIALIZED`;
- `99_SHA256SUMS.txt` binds embedded ZIP entries, not external bytes merely named by hash;
- the legacy aggregate `materialized_count` is retained for compatibility but cannot be interpreted as self-contained replay coverage.

This delta must pass its own exact-head CI before promotion.

## Required before claiming exact-head physical capability

The physical target must bind to the newest exact CI receipt tracked by issue #29. For any chosen target receipt:

- target Android version and hardware identity recorded;
- installed APK SHA-256 verified against the expected CI artifact;
- installed signing certificate captured;
- relevant hardware action actually executed;
- platform-reported sensor inventory captured;
- unavailable sensors retain typed `TOKEN_VAZIO_SENSOR_UNAVAILABLE_*` states;
- raw evidence TXT preserved;
- ZRF/CFR/PCM preserved when applicable;
- ZIPRAF generated and every embedded-entry SHA-256 verified;
- embedded bytes distinguished from externally referenced/hash-bound artifacts;
- receipt records source SHA, CI run, APK hash, device/environment and outcome.

Until those are satisfied:

```text
EXACT_HEAD_DEVICE_RUN=NOT_RUN
EXACT_HEAD_PHYSICAL_ZIPRAF=TOKEN_VAZIO
```

## Required before authorial signed release

- provider enforcement gate must pass against the actual protected ref;
- real keystore/alias/password material must be configured through the intended secret path;
- expected authorial certificate SHA-256 must be configured;
- built APK must be signed by the authorial path;
- observed certificate digest must equal the expected digest;
- signed APK digest and signing receipt must be retained;
- delivery must target the exact gated source SHA.

Current unresolved authority state:

```text
PROVIDER_ENFORCEMENT=TOKEN_VAZIO_UNRESOLVED
AUTHORIAL_SIGNING=TOKEN_VAZIO_UNTIL_CONFIGURED_AND_EXECUTED
```

## Required before independent reproducibility claim

The same-environment gate is necessary evidence but not sufficient for independent reproduction. A stronger receipt requires at least:

- a second controlled build environment that is not the same CI job/environment;
- the exact same source SHA and declared/pinned toolchain inputs;
- artifact SHA-256 comparison;
- entry-level diff if the APK differs;
- environment identity in both receipts.

Current state: `INDEPENDENT_REPRODUCTION=NOT_CLAIMED`.

## Required before acoustic calibration claims

### Relative software/analysis boundary

- known source/response alignment;
- deterministic validation of the relative 16-band profile;
- deterministic validation of the relative decay estimator;
- no promotion of a relative metric into an absolute or standards-conformant measurement.

### Relative physical evidence

- repeated CFR captures on the same controlled target setup;
- raw reference/response preserved;
- sample rate, device orientation, thermal/power state and environment recorded where material;
- repeatability statistics such as N, median, dispersion and range;
- noise/dynamic-range evidence sufficient for the intended bounded claim.

### Absolute acoustic claim

- traceable or otherwise documented physical acoustic reference;
- calibration method;
- uncertainty budget;
- physical repeatability;
- explicit separation of reference accuracy from app/software repeatability.

Current state:

```text
ABSOLUTE_SPL_REFERENCE=TOKEN_VAZIO
MEASUREMENT_UNCERTAINTY=TOKEN_VAZIO
```

## Standards boundary

ISO/NIST/ITU/EBU or other normative material may be used as engineering references or to define future test vectors. Internal CI success does not establish conformity, certification or accreditation.

`EXTERNAL_STANDARD_AUDIT=NOT_AUDITED`.

## No-go

Release notes and receipts must not convert any of the following into a stronger claim:

- `PENDING`;
- `NOT_RUN`;
- `TOKEN_VAZIO`;
- a referenced hash into embedded bytes/self-contained replay;
- same-environment reproducibility into independent reproduction;
- static installability into installed physical execution;
- pre-link freestanding core status into a claim that the final Android `.so` is bare-metal/freestanding;
- a hash into authenticity, scientific validity or calibration;
- an older executed receipt into a floating statement about a later branch head.

The obsolete `main@1a1b00... / run 36545296255` baseline is historical only. The `469eb83... / #39` receipt remains an immutable executed baseline; newer deltas append their own exact receipts rather than rewriting it.
