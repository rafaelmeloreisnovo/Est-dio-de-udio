# ASSURANCE_ZIPRAF_PIPELINE_V1

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**Baseline before this custody delta:** `main@def87c720b14ab30ff39371637530d172223da6c`, run `37013439446` = `SUCCESS`  
**Delta state:** `IMPLEMENTED_PENDING_PR_CI + claim_allowed=false`  
**External standards audit:** `NOT_AUDITED`

## 1. Purpose

The `★ VALIDAR + ZIPRAF` button composes one bounded assurance pass over material that actually exists on the device and in the current app session.

It does not certify the product and it does not turn a reference to an external standard into a conformity claim.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
REFERENCE_TO_STANDARD != AUDITED_CONFORMITY
EMBEDDED_BYTES != REFERENCED_HASH
```

## 2. Symbolic composition

The symbolic form is preserved as:

```text
★ = {†[material]} × {‡([materialized^n]) + ∅ × ∆ × § × ¶}
```

It is **not** interpreted as an invented numerical score.

| Symbol | Operational meaning |
|---|---|
| `†` | material/source identity that can be bound |
| `‡` | materialized evidence, artifacts and hashes |
| `∅` | unresolved gap / `TOKEN_VAZIO` / missing authority or reference |
| `∆` | observed behavior delta from executed probes |
| `§` | metric contract: availability, unit, execution, finite value and required reference |
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
  -> separate embedded bytes from external hash references
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

The raw evidence text is embedded under `80_raw/evidence.txt`.

## 4. ZIPRAF entries

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

Entries are emitted in lexical order using stored ZIP entries with a fixed archive-entry timestamp. `99_SHA256SUMS.txt` binds every preceding ZIP entry.

This is an integrity/custody mechanism:

```text
ZIPRAF != encryption
integrity != authenticity
hash != scientific validity
hash_reference != embedded_artifact
```

## 5. Embedded versus referenced custody

`materialized_count` is retained as a backward-compatible aggregate. It must not be read as "all bytes are inside ZIPRAF".

The current schema now makes the split explicit:

```text
materialized_count = embedded_artifact_count + referenced_artifact_count
```

Current custody classes:

| Object | Custody state |
|---|---|
| `80_raw/evidence.txt` | `EMBEDDED` |
| installed APK | `REFERENCED_NOT_EMBEDDED` |
| ZRF when a concrete SHA exists | `REFERENCED_NOT_EMBEDDED` |
| CFR when a concrete SHA exists | `REFERENCED_NOT_EMBEDDED` |
| mastered PCM when a concrete SHA exists | `REFERENCED_NOT_EMBEDDED` |
| missing ZRF/CFR/PCM | `TOKEN_VAZIO_NOT_MATERIALIZED` |

The receipt records:

```text
embedded_artifact_count=<n>
referenced_artifact_count=<n>
custody_boundary=EMBEDDED_BYTES_SEPARATE_FROM_HASH_REFERENCES
```

Therefore an evidence package can prove the hash relation to an external artifact without falsely claiming that artifact is self-contained in the ZIPRAF.

## 6. Metric qualification

A runtime metric can become `OBSERVED_METRIC_SCOPED` only when:

```text
executed
+ available when hardware-dependent
+ finite value
+ explicit unit
+ required reference present when the claim needs one
```

Otherwise the state remains one of:

```text
NOT_RUN
INSUFFICIENT_EVIDENCE
FAIL_METRIC_CONTRACT
OBSERVED_UNCALIBRATED
TOKEN_VAZIO_SENSOR_UNAVAILABLE
TOKEN_VAZIO_*
```

An unavailable physical sensor is not promoted to an observation and is not converted into metric failure merely because the hardware does not exist.

Current examples:

- accelerometer temporal delta: device-reported Android sensor scale, observation only;
- magnetometer temporal delta: device-reported microtesla scale when available; otherwise explicit unavailable state;
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

Missing coordinates do not delete a declared relationship; the edge remains explicitly unresolved or unavailable.

## 8. Claims boundary

The package can support bounded statements such as byte identity when the corresponding hash exists.

It does not automatically promote:

- an external hash reference into embedded custody;
- scientific causality;
- physical calibration without a reference;
- independent reproduction;
- standards conformity/certification/accreditation;
- product-market or performance superiority.

Those remain explicit gaps until their own evidence exists.

## 9. CI consistency gate

`ci/assurance-consistency-gate.sh` now requires source-level evidence that:

- raw evidence is counted as embedded;
- installed APK is counted as referenced, not embedded;
- aggregate materialization equals embedded + referenced;
- explicit `embedded_artifact_count` and `referenced_artifact_count` fields exist;
- installed APK custody is `REFERENCED_NOT_EMBEDDED`;
- raw evidence custody is `EMBEDDED`;
- the receipt exports the custody boundary;
- authorial signing remains fail-closed.

The gate must pass in PR CI before this delta can be promoted.

## 10. Promotion rule

```text
SOURCE
+ IMPLEMENTATION
+ TEST
+ EXECUTION
+ EVIDENCE
+ RECEIPT
!= external audit
```

A successful project CI run can prove only the executed project scope. It does not substitute for a current-head physical run, provider enforcement, authorial signing, reproducible build or external normative audit.
