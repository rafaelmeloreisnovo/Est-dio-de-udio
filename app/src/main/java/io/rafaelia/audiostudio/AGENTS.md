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
