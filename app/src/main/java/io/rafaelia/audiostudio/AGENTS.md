# AGENTS.md — Android Java platform edge

This subtree owns platform orchestration, UI and device I/O.

## Audio

Use Android APIs only as the hardware/platform edge.
Do not reimplement core DSP here when a freestanding core exists.

## Sensors / μ∆

`μ∆` means a bounded micro-delta observation step, not an SI unit.
For accelerometer vibration:

```text
Δa[k] = a[k] - a[k-1]
```

Report sampling window, sample count, effective rate and observed delta metrics.
Do not infer bearing, motor, chassis or structural defects without a validated model and reference dataset.

## Evidence bundle

The evidence writer may include:
- installed APK SHA-256;
- package/version/install/update metadata;
- source/CI coordinates embedded at build;
- runtime ABI/Android properties;
- audio and sensor inventory;
- hashes of ZRF/CFR/master artifacts;
- bounded μ∆ observations.

It must retain `TOKEN_VAZIO` when evidence is unavailable.


## Java-low policy

Prefer custom drawing and direct platform calls over UI frameworks.
Do not add AndroidX/Kotlin/third-party libraries.
Do not duplicate DSP in Java.
Provider-free local algorithms are preferred when small and testable; `LowSha256` must retain its KAT.
Java remains a platform language and necessarily uses Android/Java classes for I/O/lifecycle.

## ZIPRAF authorial lower-level Java boundary (2026-10-08)

- ZIPRAF `STORED` byte format belongs to `RfaStoredZip`. Do **not** reintroduce `ZipOutputStream`, `ZipEntry` or `CRC32` into shipped application sources; `java.util.zip` may be used in host-only interoperability tests.
- Use `RfaOrderedEntries` for the 32-entry deterministic ASCII-named manifest; do not restore `TreeMap` or `Map.Entry` solely for ZIP serialization.
- Use `RfaBoundedBytes` for up to 8 MiB of collected evidence; failure to fit is a **real error** and must not silently truncate.
- `RfaStoredZip.finish()` writes the central directory, no native method or compression provider. `OutputStream` and `MediaStore` are the explicit platform I/O boundary, not freestanding algorithms.
- On every serializer change, retain `ci/zipraf-lowlevel-gate.sh`: CRC32 KAT + independent JDK reader oracle + exact payload equality + negative name/count/size tests.
- Java class files always require a JVM/ART, and JNI must remain while the C freestanding DSP is the execution authority. Never label the APK itself bare-metal/freestanding or remove the platform edge without a tested replacement.
- New published ZIPRAF archives may have a different whole-archive hash because ZIP metadata changed. Preserve exact source→ZIP bytes→entry digests→device receipts, not historical hash equivalence.
