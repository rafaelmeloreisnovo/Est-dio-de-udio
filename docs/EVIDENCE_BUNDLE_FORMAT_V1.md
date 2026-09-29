# Evidence Bundle Format V1

Schema: `rafaelia.evidence/v1`

The evidence button writes a UTF-8 `key=value` document to:
`Downloads/RafaeliaAudio/Evidence`.

## Sections

### installed_application

- package
- version_name / version_code
- first_install_epoch_ms
- last_update_epoch_ms
- source_sha
- ci_run_id / ci_run_number
- installed_apk_sha256

### embedded_ci_provenance

When canonical CI built the APK, `ci_provenance_v1.txt` is embedded after host/ARM gates pass and before APK assembly.

The asset may state earlier gates as PASS. APK assembly/upload are intentionally not marked PASS inside that pre-build asset.

### device_runtime

Manufacturer, model, Android version, SDK and supported ABIs.

### permissions_and_audio

Microphone feature/permission and Android audio output properties.

### micro_delta_vibration

Short accelerometer observation:
- sample count;
- effective rate;
- RMS Δa;
- peak Δa;
- axis min/max;
- sensor identity.

`OBSERVED_UNPROMOTED` means data was observed but not converted into a hardware diagnosis.

### sensor_inventory

Enumerates sensors exposed by Android with type/name/vendor/version/resolution/range/min-delay.

### project_artifacts

If present, the latest ZRF, CFR and mastered PCM are bound by size and SHA-256.

## Integrity boundary

```text
APK_SHA256 -> bytes of installed APK
artifact_SHA256 -> bytes of recorded artifact
CI coordinates -> provenance address
sensor values -> runtime observations
```

None of those alone prove standards conformance, acoustic calibration or hardware health.
