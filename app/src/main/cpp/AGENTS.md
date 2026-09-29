# AGENTS.md — freestanding C cores

## Contract

C cores must remain independently compilable with:

```text
-nostdinc -ffreestanding -fno-builtin
-Wall -Wextra -Werror
```

No libc, libm, heap, filesystem, network or thread dependency.

## State

Mutable processing state belongs to caller-owned structs.
File-scope mutable state is forbidden in the core.

## ABI

Every public external symbol belongs in `native/abi/public_symbols_v1.txt`.
ARMv7 and AArch64 object gates require zero undefined symbols for each micromodule.

Private reusable primitives should prefer `static inline` internal headers when cross-object coupling would violate zero-undefined.

## Numerical policy

Fixed-point/integer is the default core representation.
Saturation, overflow boundaries and scaling must be explicit.
A numeric approximation must identify its representation and test vector.

## Evidence

A host smoke is not an ARM physical test.
An ARM object PASS is not an APK physical test.
A mathematical primitive is not an acoustic claim until tied to measured input.
