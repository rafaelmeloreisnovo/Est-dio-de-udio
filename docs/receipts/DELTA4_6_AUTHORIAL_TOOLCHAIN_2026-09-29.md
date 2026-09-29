# DELTA4.6 — Authorial CI Toolchain

```text
base_main = 5531343fb0e38167cc16eb1f8fd3df1152627044
branch = feature/authorial-ci-toolchain-v1
state = IMPLEMENTED_UNTESTED
```

## Delta

- project-owned Git checkout by exact source SHA;
- project-owned Java 17 verification;
- project-owned Android SDK package bootstrap;
- project-owned pinned Gradle bootstrap;
- Gradle 8.11.1 SHA-256 verification;
- external Action allowlist gate;
- removed checkout/setup-java/setup-android/setup-gradle Actions;
- Node application runtime remains zero;
- upload-artifact retained as isolated GitHub platform edge.

## Claim boundary

```text
AUTHORIAL_ORCHESTRATION = IMPLEMENTED_UNTESTED
AUTHORIAL_NODE_RUNTIME = NOT_IMPLEMENTED_NOT_REQUIRED
NODE_APP_RUNTIME = 0
EXTERNAL_JS_ACTIONS = upload-artifact-only
FULL_NODE_FREE_GITHUB_CI = FALSE
CI = PENDING
```

## R3

```text
F_ok = dependency surface reduced in source
F_gap = CI execution + artifact edge still external
F_next = run gate -> inspect action warnings -> promote only if complete
```
