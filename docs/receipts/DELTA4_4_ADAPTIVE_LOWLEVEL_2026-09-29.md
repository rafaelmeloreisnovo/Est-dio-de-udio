# DELTA4.4 — Adaptive UI / Low-Level Reduction

```text
base_main = 59e88a77b4cca6eee5921d5ee9ef1fec58432881
branch = feature/adaptive-lowlevel-ui-v1
state = IMPLEMENTED_UNTESTED
```

## Delta

- workstation promoted to primary UI surface;
- compact transport/evidence/tool rows;
- workspace adapts to narrow portrait vs wide/landscape;
- narrow mode uses 4x2 workspace tab grid;
- ARM packaging reduced to armeabi-v7a + arm64-v8a;
- x86_64 removed from canonical APK;
- malformed Gradle buildFeatures nesting repaired;
- R8/minification disabled in canonical release;
- resource shrinking disabled;
- explicit low-level boundary contract;
- gap/TOKEN_VAZIO ledger.

## Preserved boundary

```text
C_CORE = FREESTANDING
JAVA_ANDROID_EDGE = PRESENT
JNI = PRESENT
SDK = BUILD_TIME_PRESENT
GRADLE = BUILD_TIME_PRESENT
R8 = OFF
```

## Gate

```text
CI = PENDING
APK = PENDING
PHYSICAL_ANDROID10 = NOT_RUN
```

## R3

```text
F_ok = source delta + truthful dependency boundary
F_gap = CI + physical UI + future JNI/direct-native migration
F_next = run CI, then install on ARM32 target
```
