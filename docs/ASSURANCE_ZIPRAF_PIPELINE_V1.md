# ASSURANCE_ZIPRAF_PIPELINE_V1

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**State:** `IMPLEMENTED_PENDING_CI + claim_allowed=false`  
**External standards audit:** `NOT_AUDITED`

## 1. Purpose

The `★ VALIDAR + ZIPRAF` button composes one bounded assurance pass over the material that actually exists on the device and in the current app session.

It does not certify the product and it does not turn a reference to any external standard into a conformity claim.

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
| `†` | material/source identity that can be bound |
| `‡` | materialized evidence, artifacts and hashes |
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

The raw evidence text remains an input/evidence object and is embedded into the ZIPRAF under `80_raw/evidence.txt`.

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

Entries are emitted in lexical order using stored ZIP entries with a fixed archive-entry timestamp. `99_SHA256SUMS.txt` binds every preceding entry.

This is an integrity/custody mechanism:

```text
ZIPRAF != encryption
integrity != authenticity
hash != scientific validity
```

## 5. Metric qualification

A runtime metric can become `OBSERVED_METRIC_SCOPED` only when:

```text
executed
+ finite value
+ explicit unit
+ required reference present when the claim needs one
```

Otherwise the state remains one of:

```text
NOT_RUN
FAIL_METRIC_CONTRACT
OBSERVED_UNCALIBRATED
TOKEN_VAZIO_*
```

Current examples:

- accelerometer temporal delta: device-reported Android sensor scale, observation only;
- magnetometer temporal delta: device-reported microtesla scale, observation only;
- relative room path: bounded relative analysis, not absolute SPL;
- absolute SPL: `PENDING_PHYSICAL_REFERENCE`;
- external standards conformity: `NOT_AUDITED`.

## 6. Relation graph

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

Missing coordinates do not delete an edge; the edge remains with a `TOKEN_VAZIO` state.

## 7. Claims boundary

The package can support bounded statements such as byte identity when the corresponding hash exists.

It does not automatically promote:

- scientific causality;
- physical calibration without a reference;
- independent reproduction;
- standards conformity/certification/accreditation;
- product-market or performance superiority.

Those remain explicit gaps until their own evidence exists.

## 8. Tests

`AssurancePipelineSmoke.java` checks positive and negative state transitions, including:

- missing material cannot pass;
- no materialized evidence cannot pass;
- provenance gaps remain unpromoted;
- unresolved gaps block `★` promotion;
- a reference-dependent metric without its reference is `OBSERVED_UNCALIBRATED`;
- standards conformity remains `NOT_AUDITED` without audit evidence;
- `TOKEN_VAZIO` cannot become an identity coordinate.

The smoke is wired into `rafaeliaJavaGate`, so a PR must execute it before the Android Java gate can be treated as passing.

## 9. Promotion rule

```text
SOURCE
+ IMPLEMENTATION
+ TEST
+ EXECUTION
+ EVIDENCE
+ RECEIPT
!= external audit
```

A successful project CI run can prove only the executed project scope. It does not substitute for an external normative audit.
