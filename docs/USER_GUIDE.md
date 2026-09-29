# User Guide

## First launch

Run **Wizard / pre-flight** and grant microphone permission if recording or calibration is required.

## Recording

Use REC for raw 48 kHz PCM16 capture. The source PCM is preserved before DSP.

## Calibration

CAL provides relative speaker-room-microphone capture. Keep the phone and speaker in a stable position and use the built-in conservative level. Do not use the calibration sweep through headphones or in-ear devices.

The result is a CFR file containing the exact digital excitation and captured response.

## Evidence button

Tap **Gerar provas + teste μ∆**.

The app performs a short accelerometer observation, then writes an evidence file to:

`Downloads/RafaeliaAudio/Evidence`

The bundle can contain:
- package/version/install timestamps;
- installed APK SHA-256;
- build source/CI coordinates;
- Android and ABI information;
- audio properties;
- sensor inventory;
- μ∆ vibration metrics;
- hashes of available ZRF/CFR/master files.

Tap **Compartilhar provas** to share the latest bundle.

## Interpretation

An evidence bundle proves observed metadata and file digests. It does not prove acoustic calibration, hardware health, or standards conformance unless the specific required tests are present.
