# Low-Level Boundary V1

## Goal

Minimize hosted/runtime machinery without making false claims about Android.

## Core target

```text
C11 freestanding
fixed-point/integer
caller-owned state
heap = 0
libc = 0
libm = 0
hosted headers = 0
mutable core globals = 0
undefined core symbols = 0
```

The mathematical/data-path preference is linear memory + indices + explicit buffers. This follows the project's earlier matrix/index reduction direction.

## Android irreducible edge

A normal Android APK cannot directly own the display controller, microphone ADC, accelerometer hardware, package installation or MediaStore. Those capabilities are exposed by the Android platform.

Therefore:

```text
NO Android API at all + normal APK = contradiction
NO build tool at all + APK packaging = contradiction
NO platform ABI at all + microphone/sensor/UI = contradiction
```

The engineering target is instead:

```text
small Android edge
-> explicit bridge
-> freestanding core
```

## JNI state

JNI is currently PRESENT because Java calls the freestanding C DSP/measurement core.

`JNI_REMOVED = TOKEN_VAZIO/NOT_IMPLEMENTED`

Deleting `jni_bridge.c` without replacing the call boundary would disable the native DSP. A future direct-native/activity architecture may reduce Java/JNI further, but it remains a separately gated migration.

## SDK / Gradle

SDK and Gradle are BUILD-TIME tools in the canonical path, not DSP runtime dependencies.

Current low profile:
- only ARMv7 + AArch64 packaged;
- R8 disabled;
- resource shrinking disabled;
- no third-party runtime libraries declared;
- Java uses platform SDK only;
- C core retains freestanding flags.

## R8

`R8_CANONICAL_PATH = OFF`.

No minification/shrinking is needed to produce the canonical low-level test APK.

## Evidence

A dependency is only marked removed after:
1. source/build reference removed;
2. build succeeds;
3. APK/package inspection confirms the intended absence;
4. physical behavior is retested when the removed layer affected hardware I/O.
