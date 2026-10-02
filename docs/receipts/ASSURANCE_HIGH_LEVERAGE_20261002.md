# RAFAELIA Audio — Assurance High-Leverage Closure — 2026-10-02

## Invariant

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
```

## Closed in source

1. `20_materialized.json` now explicitly enumerates the installed APK SHA-256, matching the base `materializedCount = 2` definition: installed APK + raw runtime evidence.
2. `authorial_signing` is now an explicit ZIPRAF gap whenever the runtime evidence does not prove `signing_cert_matches_expected=PASS` under `AUTHORIAL_ANDROID_APKSIGNER` with a concrete signer identity and expected certificate.
3. `60_claims.json` now keeps `authorial_signing_identity` scoped and fail-closed.
4. `70_receipt.txt` now reports `authorial_signing_binding` explicitly.
5. A fail-closed source consistency gate checks the ZIPRAF count/signing semantics before Java/APK promotion.
6. A post-assembly ELF boundary gate separates freestanding core-object evidence from the final Android shared object and permits only the current Android platform link edge (`libc.so`, `libm.so`, `libdl.so`).
7. Component origin now records `core_zero_undefined_scope=FREESTANDING_CORE_OBJECTS_PRE_ANDROID_LINK` and `final_android_so_scope=ANDROID_PLATFORM_LINKED_SHARED_OBJECT_POST_LINK`.

## Still external / not closed by source

- `main` provider protection / required checks: provider-admin action required; source cannot claim it is enabled.
- authorial keystore and expected certificate: real secret/identity material required.
- independent physical reproduction: second controlled install/device receipt required.
- absolute SPL: traceable physical acoustic reference required.
- external standards conformity: `NOT_AUDITED`.

## Promotion rule

```text
SOURCE_CHANGE -> PR_CI_PASS -> MAIN_CI_PASS -> EXACT_BINARY -> PHYSICAL_EXECUTION -> NEW_ZIPRAF
```

No physical or signing gap is promoted merely because source/CI passes.

## R3

```text
F_ok = materialized ontology + signing gap ontology + core/final-ELF scope materialized in source
F_gap = provider protection + authorial secret/cert + independent physical reproduction + physical SPL reference + external audit
F_next = CI this branch -> merge only if green -> main CI -> signed release only after provider protection + secrets -> physical receipt
```
