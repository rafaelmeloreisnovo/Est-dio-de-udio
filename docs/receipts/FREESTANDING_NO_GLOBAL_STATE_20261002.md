# FREESTANDING NO-GLOBAL-STATE HARDENING — 2026-10-02

## Intent

Reduce hidden state, symbol friction, and ambiguity in the freestanding C micromodules without pretending that the Android/JNI shared-object edge is freestanding.

## Scope

Applied to the existing freestanding micromodule set compiled by `ci/freestanding-runtime-gate.sh` for:

- `armv7a-linux-androideabi29`
- `aarch64-linux-android29`

The Android/JNI edge remains a separate platform-linked layer.

## New fail-closed invariants

```text
UNDEFINED_RUNTIME_SYMBOLS=0
WRITABLE_PERSISTENT_SYMBOLS=0
COMMON_SYMBOLS=0
LOCAL_SHADOWING=COMPILER_ERROR
READ_ONLY_NAMED_CONSTANTS=ALLOWED
AUTOMATIC_LOCAL_STATE=STACK_OR_REGISTER_SCOPE
```

`llvm-nm` rejects object symbols in writable/common storage classes (`B/b`, `C/c`, `D/d`, `G/g`, `S/s`). This closes the gap left by source-only checks because a non-static global or function-local static would still materialize as persistent writable object state.

`-Wshadow -Werror` rejects local identifiers that silently mask another coordinate.

## Meaning of “without naming variables”

Literal source code without local identifiers is not a useful or general C invariant. The enforceable low-level interpretation used here is:

- no named writable global/static state in the freestanding object;
- no hidden common storage;
- no shadowed local state;
- state flows through parameters, caller-owned structures, automatic locals, and return values;
- read-only lookup tables/constants may remain named because they are immutable data, not hidden mutable state.

## Evidence boundary

This receipt is IMPLEMENTED_UNTESTED until CI executes the modified gate on both ABIs.

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != 0
IMPLEMENTED_UNTESTED != PASS
```

## R3

```text
F_ok = gate materialized in source
F_gap = CI execution on ARMv7+AArch64
F_next = PR CI -> fix any real shadow/global-state findings -> merge only if green
```
