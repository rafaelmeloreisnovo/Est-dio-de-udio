# AGENTS.md — Rafaelia Audio Studio

## Authority

This file governs repository-wide work. More specific `AGENTS.md` files govern their own subtrees.

## Canonical invariant

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
OBSERVED_UNPROMOTED != PASS
```

Never replace missing evidence with a plausible value.

## Route

```text
INTENT -> CURRENT_STATE -> READ_MIN -> ROUTE -> SOURCE_MIN
-> AUTHORITY -> ACT -> EVIDENCE -> WRITE_MIN -> R3
```

Read the smallest authoritative surface first. Expand only for a missing source, contradiction, unresolved authority, missing evidence, or explicit request.

## Repository roles

- Android/Java: platform UI, microphone, storage, codecs, sensors and packaging.
- JNI: narrow platform-to-core boundary.
- C core: authorial freestanding fixed-point algorithms.
- docs: contracts, claims, protocols, receipts and publication.
- workflows: reproducible gates; workflow PASS is evidence only for executed gates.

## Claim rules

A feature can be documented as implemented only when the source exists.
A build can be documented as PASS only after the matching run completed successfully.
Physical microphone/speaker/sensor claims require a physical receipt.
Absolute SPL requires a physical acoustic reference.
Sensor vibration observations are not hardware-fault diagnoses.

## Change discipline

1. Preserve raw/source artifacts.
2. Prefer small micromodules and explicit state.
3. Keep Android services outside freestanding C.
4. Extend ABI manifest when adding a public C symbol.
5. Add or update smoke vectors for new primitives.
6. Record material FAIL -> correction -> PASS chains.
7. End substantive work with `R3=<F_ok,F_gap,F_next>`.

## Licensing

New source should carry:
`SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1`.

Research/evaluation rights and commercial rights are governed by the repository license documents; technical controls do not substitute for legal enforcement.
