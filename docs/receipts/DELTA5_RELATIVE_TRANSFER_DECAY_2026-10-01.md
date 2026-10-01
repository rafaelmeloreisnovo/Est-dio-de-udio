# Delta 5 — Relative transfer + decay closure — 2026-10-01

## Authority

```text
repository = rafaelmeloreisnovo/Est-dio-de-udio
baseline_main = 1a1b00bb93b0988224d8b24085a82029d1228c45
baseline_main_ci_run = 36545296255
baseline_main_ci = PASS
source = uploaded Est-dio-de-udio-main.zip + GitHub main
```

## Delta

- 16-band relative energy profile over the aligned exponential sweep;
- backward-integrated relative decay with tail-noise estimate;
- explicit EDT/T20/T30 candidate flags when dynamic range permits;
- `SPEC` and `ROOM` CFR chunks while retaining both PCM source streams;
- UI plots the measured relative profile instead of a placeholder;
- evidence bundle records the relative analysis and its claim boundary;
- relative calibration failure path resets derived state fail-closed;
- JNI relative-transfer result initialized before failure paths.

## Local gates

```text
host_dsp_smoke = PASS
host_meter_smoke = PASS
host_manifold_smoke = PASS
low_sha256_kat = PASS
freestanding_source_gate = PASS
remote_pr_ci = PENDING
android_apk_build_for_delta5 = PENDING_REMOTE_CI
```

## Claims deliberately not promoted

```text
physical_android_capture = NOT_RUN_IN_THIS_EXECUTION
absolute_spl = PENDING_PHYSICAL_REFERENCE
iso3382 = NOT_CLAIMED
ir_deconvolution = NOT_CLAIMED
room_correction = PENDING
real_authorial_signed_release = TOKEN_VAZIO_UNTIL_SECRETS_CONFIGURED
```

`IMPLEMENTED_LOCAL_TESTED != REMOTE_CI_PASS` until GitHub verifies this delta.
