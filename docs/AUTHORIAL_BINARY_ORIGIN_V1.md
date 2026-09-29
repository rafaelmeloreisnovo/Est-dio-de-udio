# Authorial Binary Origin V1

## Core rule

```text
PROJECT_AUTHORED != PLATFORM != TOOLCHAIN != GENERATED_BINARY
```

The project may claim authorship only over source, formats, schemas, tests, documentation and generated data that are actually produced under project authority.

It does **not** relabel Android, Java, Gradle, AGP, NDK, CMake, Clang, Git or GitHub infrastructure as authorial.

## Build-time origin asset

Every canonical build generates `component_origin_v1.txt` before APK assembly.

It binds:

- project Java source tree digest;
- project C source tree digest;
- project package;
- Android Gradle Plugin version;
- Gradle version;
- Java/Gradle/Clang binary paths;
- origin classification rules.

## Post-build binary receipt

The canonical pipeline generates:

```text
binary_origin_receipt_v1.txt
apk-entry-sha256.txt
```

The APK entry manifest hashes each concrete ZIP/APK entry.

Entries are classified by provenance category:

```text
classes.dex
  = GENERATED_DEX_MIXED_PROJECT_BYTECODE_PLATFORM_REFERENCES

lib/*/librafaelia_audio.so
  = PROJECT_NATIVE_BINARY_WITH_ANDROID_ABI_EDGE

AndroidManifest.xml
  = GENERATED_FROM_PROJECT_MANIFEST

resources.arsc
  = GENERATED_ANDROID_RESOURCE_TABLE

META-INF/*
  = BUILD_OR_SIGNATURE_METADATA
```

## Meaning of “each bit”

A bit can be bound to a byte-level digest, but authorship is a semantic/legal provenance property, not a property that can be inferred from a bit value.

Therefore:

```text
BYTE_HASH = identity/integrity coordinate
SOURCE_PROVENANCE = authorship coordinate
TOOLCHAIN_ORIGIN = transformation coordinate
SIGNATURE = key-holder authentication coordinate
```

No external byte is reclassified as project-authored merely because it appears inside the final APK.
