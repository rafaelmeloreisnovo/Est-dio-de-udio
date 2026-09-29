# DELTA4.7 — Authorial Binary / Sensor Permission / Signing / Layout

```text
base_main = efba29fd958bf4341715dadb79167f77f8b0c733
branch = feature/authorial-binary-signing-layout-v1
state = IMPLEMENTED_UNTESTED
```

## Source delta

- adaptive ninth SYS workspace;
- dynamic 3/5/9-column tab layout;
- optional ACTIVITY_RECOGNITION request only from SYS;
- runtime signing-certificate SHA-256 evidence;
- embedded component-origin manifest;
- embedded permission contract;
- per-APK-entry SHA-256 binary manifest;
- zero external JavaScript Actions in canonical workflow;
- GitHub CLI prerelease publication;
- fail-closed authorial APK signing workflow;
- governed RafGitTools Pages receipt publication path.

## Protected boundaries

```text
EXTERNAL_PLATFORM_BYTES != PROJECT_AUTHORED
EXTERNAL_TOOLCHAIN_BYTES != PROJECT_AUTHORED
DEBUG_CERTIFICATE != AUTHORIAL_CERTIFICATE
REAL_SIGNING_VARIABLES = TOKEN_VAZIO until configured in GitHub
```

## Gate

```text
CI = PENDING
SYS_UI_PHYSICAL = NOT_RUN
ACTIVITY_RECOGNITION_PHYSICAL = NOT_RUN
SIGNED_RELEASE = NOT_RUN
PAGES_SIGNING_RECEIPT = TOKEN_VAZIO
```
