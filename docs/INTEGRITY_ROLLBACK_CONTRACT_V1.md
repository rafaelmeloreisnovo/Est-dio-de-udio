# INTEGRITY / ABI / ROLLBACK CONTRACT V1

Copyright (c) 2026 Rafael Melo Reis
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**State:** `CI_ENFORCED_PARTIAL`

## 1. Purpose

Changes to loops, symbols or micromodule boundaries must become observable. The project does not pretend that a binary can legally or physically “undo any alteration” by magic.

Instead:

```text
change
-> compile gate
-> undefined-symbol gate
-> ABI comparison
-> deterministic smoke vectors
-> APK build
-> PASS/FAIL
```

## 2. Current controls

The PR/main workflows now include the manifold sources in:

- `-ffreestanding -fno-builtin -nostdinc` host compilation;
- forbidden hosted/runtime source scan;
- mutable file-scope-state gate;
- ARMv7 zero-undefined inspection;
- AArch64 zero-undefined inspection;
- `manifold_smoke` round-trip tests;
- Android APK build.

## 3. Rollback

The authoritative rollback mechanism is Git:

```text
known_good_commit
+ branch/ref
+ CI receipt
+ optional artifact hash
```

A failing candidate must not be promoted. Reverting to a known-good ref is explicit and auditable.

Runtime fallback may be added for a specific DSP operation only when both old and new implementations are intentionally shipped and equivalence/failure conditions are defined.

## 4. License boundary

Integrity checks detect technical drift. The research/commercial license defines permissions.

```text
TECHNICAL_INTEGRITY != LEGAL_ENFORCEMENT
```

Source hashing must never be described as automatically proving infringement, authorship or contractual breach.

## 5. Future stronger gate

Next candidate:

- per-micromodule ABI allowlist;
- deterministic object digest matrix for ARMv7/AArch64/x86_64;
- performance envelope;
- property/fuzz vectors;
- signed release receipt;
- reproducible build record.

Until those execute:

```text
INTEGRITY_LEVEL = PARTIAL
claim_allowed = false for reproducible-build equivalence
```
