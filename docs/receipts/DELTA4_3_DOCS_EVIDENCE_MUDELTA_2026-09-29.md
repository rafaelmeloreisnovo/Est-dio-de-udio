# DELTA4.3 — Documentation, Evidence Bundle and μ∆ Sensor Receipt

Copyright (c) 2026 Rafael Melo Reis  
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

```text
base_main = 452de8d1276137b093b05fc2d956f565757620db
branch = feature/docs-agents-evidence-sensors-v1
kind = implementation_receipt
```

## Source delta

- scoped AGENTS governance at repository/app/C/Java/docs/CI levels;
- canonical START HERE and publication/document-product indexes;
- product, user, developer, verification, installation, hardware, μ∆,
  release, security, glossary and FAQ documents;
- BuildConfig source/CI provenance fields;
- CI-generated provenance asset before APK assembly;
- optional accelerometer declaration;
- bounded `MicroDeltaVibrationProbe`;
- `EvidenceBundleWriter` with installed APK SHA-256;
- UI actions to generate/share evidence.

## μ∆ definition

```text
Delta a[k] = a[k] - a[k-1]
state = OBSERVED_UNPROMOTED
hardware_fault_diagnosis = NOT_CLAIMED
```

## Current gate

```text
SOURCE = PRESENT
DOCUMENTATION = PRESENT
JAVA_BUILD = PENDING_CI
ARMV7_CORE_REGRESSION = PENDING_CI
AARCH64_CORE_REGRESSION = PENDING_CI
APK_BUILD = PENDING_CI
PHYSICAL_ANDROID10 = NOT_RUN
EVIDENCE_BUTTON_PHYSICAL = NOT_RUN
ACCELEROMETER_PHYSICAL = NOT_RUN
```

## R3

```text
F_ok =
  documentation family
  + scoped AGENTS
  + evidence bundle source
  + installation APK hash route
  + μ∆ accelerometer source

F_gap =
  CI
  + physical evidence generation
  + real sensor receipt
  + installed APK evidence bundle

F_next =
  CI -> install APK -> tap evidence button
  -> preserve first on-device evidence bundle
```
