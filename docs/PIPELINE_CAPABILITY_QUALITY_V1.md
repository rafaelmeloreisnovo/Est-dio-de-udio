# PIPELINE_CAPABILITY_QUALITY_V1

**State:** `IMPLEMENTED_PENDING_EXACT_HEAD_CI`  
**External standards audit:** `NOT_AUDITED`

## Purpose

Reduce CI surface while increasing operational clarity. The pipeline is treated as a work formation: each phase must create a bounded capability before the next phase can promote the execution.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
REFERENCE_TO_STANDARD != AUDITED_CONFORMITY
```

This is an internal engineering method. It does not assert certification, accreditation, or external standards conformity.

## Formation

```text
00 PLAN / ROUTE
  ↓
10 EXACT SOURCE
  ↓
20 CAPACITY BOOTSTRAP
  ↓
30 SINGLE-ROOT TOPOLOGY
  ↓
40 QUALITY CAPABILITY
  ↓
50 ARCHITECTURE / ABI
  ↓
60 EVIDENCE PREPARATION
  ↓
70 BUILD
  ↓
80 RECEIPT
  ↓
90 DELIVERY (conditional)
  ↓
99 SCOPED SUMMARY
```

## Operational principles

### Clean

- exactly one active workflow root: `.github/workflows/START.yml`;
- phase logic lives in `ci/rafaelia-pipeline.sh`, not duplicated across YAML files;
- release modes are routes of the same pipeline, not independent CI universes;
- Git history is the rollback surface for retired workflow files.

### Practical

The same pipeline serves three normal operations:

| Event | Route |
|---|---|
| pull request | validate |
| push to `main` | validate → live debug delivery |
| manual dispatch | validate, live-debug, or signed-release |

An optional exact 40-character source SHA can be supplied to manual dispatch. If omitted, the selected workflow ref SHA is used.

### Fast

- PR execution performs one bootstrap and one canonical gate path;
- no external JavaScript GitHub Actions are required;
- expensive delivery work is conditional and happens only after the gate succeeds;
- PR concurrency cancels an older in-progress run for the same ref;
- signed delivery does not repeat quality analysis after the exact source gate has passed in the same pipeline run.

### Fail-closed

A phase failure blocks every dependent phase. Missing identity, signing material, physical reference, metric contract, or execution remains explicit rather than being converted into success.

## Phase contracts

### 00 — PLAN / ROUTE

Outputs:

```text
source_sha=<exact 40-char lowercase SHA>
delivery=validate|live-debug|signed-release
```

Invalid event, SHA, or delivery mode fails before source execution.

### 20 — CAPACITY BOOTSTRAP

Materializes the project-owned Java, Android and Gradle toolchain route and prints the toolchain report.

### 30 — SINGLE-ROOT TOPOLOGY

Requires:

```text
ACTIVE_WORKFLOW_COUNT=1
CANONICAL_WORKFLOW=.github/workflows/START.yml
EXTERNAL_JS_ACTIONS=NONE
NODE_APP_RUNTIME=0
```

A second `.yml`/`.yaml` file under `.github/workflows` becomes a hard failure.

### 40 — QUALITY CAPABILITY

Executes:

- Gradle authorial contract;
- Java compile gate and Assurance smoke;
- DSP smoke;
- meter smoke;
- manifold smoke;
- freestanding source restrictions;
- Java/Gradle dependency boundary;
- canonical JNI edge check.

The maximum promotion from this phase is `PASS_EXECUTED_SCOPE`.

### 50 — ARCHITECTURE / ABI

Executes the ARMv7 and AArch64 freestanding object checks. ARMv7 also compares the public ABI manifest.

### 60 — EVIDENCE PREPARATION

Executes LowSha256 known-answer verification, generates component/origin assets, and binds exact source/run coordinates into the app provenance asset.

The asset explicitly records:

```text
external_standard_audit=NOT_AUDITED
claim_allowed=false
```

### 70 — BUILD

Builds the exact debug APK from the already gated source.

### 80 — RECEIPT

Hashes the APK and every non-directory ZIP/APK entry and writes `rafaelia.binary-origin/v1` evidence.

### 90 — DELIVERY

`validate`: no publishing.

`live-debug`: after the gate, rebuilds the exact SHA under a write-scoped job, re-materializes provenance, generates the binary receipt and publishes a prerelease.

`signed-release`: after the gate, obtains signing coordinates only from configured variables/secrets, builds release, signs/verifies through the existing authorial signing script, publishes the signed prerelease, then publishes the verification receipt to the configured Pages source.

Write permission is isolated to delivery jobs. The PR quality gate remains `contents: read`.

## Quality promotion boundary

A green pipeline may establish only the executed project scope.

```text
CI_PASS != DEVICE_EXECUTION
CI_PASS != PHYSICAL_CALIBRATION
CI_PASS != INDEPENDENT_REPRODUCTION
CI_PASS != EXTERNAL_AUDIT
```

Device and physical claims still require their own evidence.

## Rollback

The refactor does not rewrite history. Rollback is a normal revert of the pipeline migration commits/merge. The previous workflows remain recoverable from Git history at the pre-refactor main SHA.
