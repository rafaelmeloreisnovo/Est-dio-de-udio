# AGENTS.md — app/

The Android application is a platform shell around auditable cores.

## Allowed here

Android SDK, Gradle, JNI packaging, permissions, MediaStore, AudioRecord, AudioTrack, SensorManager and UI.

## Required boundaries

- Do not describe the full APK as freestanding.
- Preserve raw PCM before destructive DSP.
- Hardware I/O must report capability/fallback state.
- Missing microphone/sensor/reference = `TOKEN_VAZIO` or explicit unsupported state.
- A generated evidence bundle may report installation/runtime facts, but must not infer a fault from them.

## Build provenance

`BuildConfig.SOURCE_SHA`, CI run ID and run number are provenance coordinates, not proof of all claims.
The embedded CI asset only states gates that executed before APK assembly.

## UX

Actions with physical effects must be explicit.
Calibration uses conservative speaker level and is not intended for headphones/in-ear use.
