# Parser Fuzz + ARM Runtime Gate V1

## Intent

Reduce two recurring high-impact risks without overstating physical evidence:

1. untrusted ZRF/CFR/RAC1 input bounds;
2. ARMv7 freestanding regressions that introduce compiler runtime helpers such as `__aeabi_*div*`.

Canonical invariant:

```text
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
IMPLEMENTED_UNTESTED != PASS
INSTALLABLE_STATIC != INSTALLED_PHYSICAL
```

## Parser/decoder fuzz gate

`ci/parser-fuzz-asan.sh` compiles `native/tests/parser_fuzz.c` with host Clang:

```text
-fsanitize=address,undefined
-fno-omit-frame-pointer
-Wall -Wextra -Werror
```

The deterministic corpus performs 8192 bounded mutations across:

- ZRF/CFR container header reads;
- chunk-header length boundaries;
- RAC1 decoder random/truncated input;
- output guard sentinels;
- exact RAC1 round-trip;
- shared integer-division boundary cases.

This is defensive host evidence. It is not proof that all possible malformed inputs are safe.

## ARM runtime gate

`ci/freestanding-runtime-gate.sh` compiles every freestanding micromodule for:

- `armv7a-linux-androideabi29`;
- `aarch64-linux-android29`.

Every object must have zero undefined external symbols. The gate also contains an explicit regression sentinel for historical integer-division runtime helpers.

A shared header-only primitive, `rfa_int_math.h`, now owns bounded signed-64 / unsigned-64 shift/subtract division used by NLMS. It explicitly handles the signed `INT64_MIN` magnitude case without evaluating `-INT64_MIN`.

## Installation boundary

Current main already has a separate static APK gate checking ZIP integrity, Android signature structure, zipalign, package id, min/target SDK and both phone ABIs.

```text
INSTALLABLE_STATIC = evidence from CI only
INSTALLED_PHYSICAL = requires adb/package-manager receipt on a real Android device
```

## Provider boundary

Repository source cannot turn on GitHub provider branch protection. A signed-release route is fail-closed when `main` is not provider-protected, but the provider control itself remains external to the repository.

```text
MAIN_PROVIDER_PROTECTION = unresolved until provider readback says protected=true
AUTHORIAL_SIGNING_KEY = TOKEN_VAZIO until real keystore/certificate variables exist
```

## Claim state

Before exact-head CI:

```text
PARSER_ASAN = IMPLEMENTED_UNTESTED
PARSER_UBSAN = IMPLEMENTED_UNTESTED
ARM_RUNTIME_GATE = IMPLEMENTED_UNTESTED
APK_INSTALLABLE_STATIC = prior-main PASS only
ANDROID10_PHYSICAL_INSTALL = NOT_RUN
claim_allowed = false
```

Promote only gates actually executed by the exact PR head.
