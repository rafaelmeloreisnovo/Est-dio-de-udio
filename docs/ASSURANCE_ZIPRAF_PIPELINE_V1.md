# ASSURANCE_ZIPRAF_PIPELINE_V1

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**Executed software baseline:** `IMPLEMENTED + CI_GATE_PASS` for `main@469eb83b0d23286190c306b17c3fd378118dadd4`, run `37014682335` / #39.  
**Receipt semantics:** the exact SHA/run/hash is immutable executed evidence, not a floating `main` pointer.  
**Exact-head physical ZIPRAF:** `NOT_RUN / TOKEN_VAZIO` until issue #29 binds the newest target APK to a device.  
**External standards audit:** `NOT_AUDITED`.  
**Claim policy:** `claim_allowed=false`.

## 1. Purpose

The `★ VALIDAR + ZIPRAF` button composes one bounded assurance pass over material that actually exists on the device and in the current app session.

It does not certify the product and it does not turn a reference to an external standard into a conformity claim.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
REFERENCE_TO_STANDARD != AUDITED_CONFORMITY
```

## 2. Symbolic composition

The requested symbolic form is preserved as:

```text
★ = {†[material]} × {‡([materialized^n]) + ∅ × ∆ × § × ¶}
```

It is **not** interpreted as an invented numerical score.

| Symbol | Operational meaning |
|---|---|
| `†` | source/material identity that can be bound |
| `‡` | evidence coverage: embedded bytes and/or explicitly hash-bound material coordinates |
| `∅` | unresolved gap / `TOKEN_VAZIO` / missing authority or reference |
| `∆` | observed behavior delta from executed probes |
| `§` | metric contract: unit, execution, finite value and required reference |
| `¶` | typed provenance/relationship edge |
| `★` | fail-closed state derived from those predicates |

`star_numeric_score=TOKEN_VAZIO_NOT_DEFINED` unless a future, separately justified scoring model defines calibrated weights and validation evidence.

## 3. One-click sequence

```text
button
  -> run vibration probe
  -> run magnetometer probe
  -> write raw installed evidence
  -> hash installed APK
  -> hash available ZRF/CFR/master PCM
  -> qualify metric states
  -> materialize relations
  -> materialize gaps + exit criteria
  -> materialize bounded claims
  -> generate receipt
  -> generate per-entry SHA-256 manifest
  -> write ordered ZIPRAF container
```

The primary output is written to:

```text
Downloads/RafaeliaAudio/Assurance/rafaelia_assurance_<epoch>.zipraf
```

## 4. Custody classes: embedded vs referenced

The ZIPRAF V1 must not equate a recorded hash with embedded object bytes.

### Embedded bytes

The current container embeds the raw evidence text under:

```text
80_raw/evidence.txt
```

The other JSON/TXT receipt entries are also actual ZIP entries and are covered by `99_SHA256SUMS.txt`.

### Hash-bound / referenced material

Installed APK, ZRF, CFR and mastered PCM may be represented by identity fields, paths and SHA-256 coordinates without their complete bytes being copied into this ZIPRAF. Such a coordinate proves only what the captured evidence can bind; it is not equivalent to self-contained replay material.

Therefore:

```text
EMBEDDED_BYTES != REFERENCED_HASH
HASH_REFERENCE != EMBEDDED_ARTIFACT
INTEGRITY != AUTHENTICITY
HASH != SCIENTIFIC_VALIDITY
```

The existing `materialized_count` field is retained for V1 compatibility as a **coverage coordinate**, not an `embedded_artifact_count`. A future schema revision may split explicit counters without silently redefining V1.

## 5. ZIPRAF entries

```text
00_manifest.json
10_material.json
20_materialized.json
30_metrics.json
40_relations.json
50_gaps.json
60_claims.json
70_receipt.txt
80_raw/evidence.txt
99_SHA256SUMS.txt
```

Entries are emitted in lexical order using stored ZIP entries with a fixed archive-entry timestamp. `99_SHA256SUMS.txt` binds every preceding **embedded ZIP entry**.

This is an integrity/custody mechanism:

```text
ZIPRAF != encryption
integrity != authenticity
hash != scientific validity
```

## 6. Metric qualification

A runtime metric can become `OBSERVED_METRIC_SCOPED` only when:

```text
executed
+ finite value
+ explicit unit
+ required reference present when the claim needs one
```

Otherwise the state remains one of the typed non-promoted states, including:

```text
NOT_RUN
INSUFFICIENT_EVIDENCE
FAIL_METRIC_CONTRACT
OBSERVED_UNCALIBRATED
TOKEN_VAZIO_SENSOR_UNAVAILABLE_*
TOKEN_VAZIO_*
```

Hardware absence is orthogonal to metric validity: an unavailable sensor is not promoted to an observation and is not converted into `FAIL_METRIC_CONTRACT` merely because the device lacks that hardware.

Examples:

- accelerometer temporal delta: device-reported Android sensor scale, observation only after physical execution;
- magnetometer temporal delta: device-reported microtesla scale when available; typed unavailable state otherwise;
- relative room path: bounded relative analysis, not absolute SPL;
- absolute SPL: `PENDING_PHYSICAL_REFERENCE`;
- external standards conformity: `NOT_AUDITED`.

## 7. Relation graph

The package records typed edges such as:

```text
source_sha --BUILDS--> installed_apk
ci_run_id --EXECUTES_BUILD_GATE--> installed_apk
installed_apk --EXECUTES--> raw_evidence
raw_evidence --OBSERVES--> vibration_delta
raw_evidence --OBSERVES--> magnetic_delta
zrf --MATERIALIZES--> capture_path
cfr --MATERIALIZES--> relative_calibration_path
pcm --MATERIALIZES--> master_path
metrics --BOUNDS--> claims
```

Missing coordinates do not become fabricated observations. They remain typed `TOKEN_VAZIO` / unavailable / not-run states according to the actual failure or absence mode.

## 8. Executed software receipt

Run #39 for `main@469eb83b0d23286190c306b17c3fd378118dadd4` executed the software assurance path and recorded, among other gates:

```text
ASSURANCE_CONSISTENCY_GATE=PASS_EXECUTED_SCOPE
ASSURANCE_PIPELINE_SMOKE=PASS
SENSOR_UNAVAILABLE_SEMANTICS=PASS
COMPILE_DEBUG_GATE=PASS_EXECUTED_SCOPE
PARSER_ASAN=PASS
PARSER_UBSAN=PASS
FREESTANDING_RUNTIME_GATE=PASS
FINAL_ELF_BOUNDARY_GATE=PASS_EXECUTED_SCOPE
INSTALLABLE_STATIC=PASS
BINARY_RECEIPT=PASS
```

The same source was rebuilt after a clean in the same pinned CI environment:

```text
REPRO_BUILD_A_SHA256=1656df3ed0aed0038162400fc9734b4856a465539a513593172edc53c32f1c43
REPRO_BUILD_B_SHA256=1656df3ed0aed0038162400fc9734b4856a465539a513593172edc53c32f1c43
APK_REPRODUCIBLE_SAME_ENV=PASS
INDEPENDENT_REPRODUCTION=NOT_CLAIMED
```

This receipt does not supply device execution. Its exact APK remains `INSTALLED_PHYSICAL=NOT_RUN`. If `main` advances after this receipt, the next physical target must use the newer exact SHA/run/APK hash tracked by issue #29 rather than treating this receipt as a floating pointer.

## 9. Android/freestanding boundary

The pre-Android-link authorial core object scope is separately gated for ARMv7/AArch64 zero undefined symbols and no writable persistent named state.

The final Android shared library is intentionally classified as a hosted platform edge:

```text
FINAL_ELF_CLASS=ANDROID_PLATFORM_LINKED
FINAL_ELF_ALLOWED_NEEDED=libc.so,libm.so,libdl.so
FINAL_ELF_TRUE_FREESTANDING=NO
```

Therefore a freestanding core claim must never be broadened into a statement that the final APK `.so` is bare-metal or freestanding.

## 10. Claims boundary

The package can support bounded statements such as byte identity when the corresponding bytes/hashes and provenance exist.

It does not automatically promote:

- scientific causality;
- physical calibration without a reference;
- exact-head physical execution without device evidence;
- same-environment reproducibility into independent reproduction;
- authorial authenticity from debug signing;
- standards conformity/certification/accreditation;
- product-market or performance superiority.

Those remain explicit gaps until their own evidence exists.

## 11. Promotion rule

```text
SOURCE
+ IMPLEMENTATION
+ TEST
+ EXECUTION
+ EVIDENCE
+ RECEIPT
!= external audit
```

A successful project CI run proves only its executed project scope. It does not substitute for provider enforcement, physical evidence, independent reproduction, signing authority, calibration or an external normative audit.
