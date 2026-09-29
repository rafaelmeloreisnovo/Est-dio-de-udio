# Developer Guide

## Entry points

- `app/src/main/cpp/` — freestanding signal cores.
- `app/src/main/java/io/rafaelia/audiostudio/` — Android platform edge/UI.
- `native/tests/` — deterministic host smoke vectors.
- `native/abi/public_symbols_v1.txt` — C ABI manifest.
- `.github/workflows/` — reproducible gates.
- `docs/receipts/` — append-only evidence.

## New C micromodule checklist

1. own `.c/.h`;
2. no system headers;
3. caller-owned mutable state;
4. no undefined symbols on ARMv7/AArch64;
5. public symbols registered in ABI manifest;
6. host smoke vector;
7. CMake inclusion;
8. workflow inclusion;
9. documentation contract;
10. receipt after execution.

## New Android capability checklist

1. identify why platform API is required;
2. optional hardware capability must fail gracefully;
3. no synthetic measurement on absent hardware;
4. record runtime source/fallback;
5. preserve raw evidence when feasible;
6. avoid upgrading observations into claims.

## Development evidence

CI embeds a provenance asset after native gates pass and before APK assembly. The installed app can export those coordinates alongside the SHA-256 of the installed APK.
