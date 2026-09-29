# LOW_LEVEL_BOUNDARY_V1

## Goal

Push every algorithm that can be platform-independent into small auditable cores, while keeping only the irreducible Android shell at the edge.

## Achieved

```text
CORE_SYSTEM_HEADERS = 0
CORE_LIBC = 0
CORE_LIBM = 0
CORE_HEAP = 0
CORE_THREADS = 0
CORE_FILESYSTEM = 0
CORE_NETWORK = 0
JAVA_THIRD_PARTY_DEPS = 0
ANDROIDX = 0
KOTLIN = 0
R8_SHRINK = 0
JNI_IMPLEMENTATION_FILES = 1
PHONE_ABIS = armeabi-v7a + arm64-v8a
```

The evidence SHA-256 path uses the project-local integer/bitwise `LowSha256` instead of `java.security.MessageDigest`.

## Irreducible platform shell

An Android application still requires Android platform contracts for:

- process/activity lifecycle;
- screen/touch;
- microphone and speaker;
- sensors;
- storage/content URIs;
- package installation;
- DEX/APK packaging.

Therefore:

```text
ANDROID_PLATFORM_API != optional for Android app
BUILD_TOOLCHAIN != runtime dependency
JNI_EDGE != DSP dependency
```

The project keeps JNI confined to `jni_bridge.c`. If the C core is removed from the APK, JNI can disappear, but then the app is no longer executing that C core.

## Build tools

Gradle/SDK/NDK are build-time tooling for the Android artifact, not signal-processing dependencies.

The freestanding C cores are independently compiled by CI with Clang flags before Gradle is invoked. R8/minification is disabled.

A future custom APK packer/compiler path may reduce build orchestration, but it cannot remove Android's required binary/package formats.

## Java policy

Java is a thin platform edge:

- no AndroidX;
- no Kotlin;
- no third-party libraries;
- no crypto provider for evidence SHA-256;
- no signal DSP duplicated in Java;
- custom-drawn studio console reduces widget hierarchy;
- physical I/O remains explicit Android API use.

## Claim

```text
"zero external DSP/runtime library dependencies in core" = supported
"Android APK has zero platform dependency" = false by definition
```
