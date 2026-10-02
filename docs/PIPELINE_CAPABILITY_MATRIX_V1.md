# PIPELINE_CAPABILITY_MATRIX_V1

| Phase | Capability | Input | Evidence produced | Failure state |
|---|---|---|---|---|
| 00 PLAN | deterministic routing | event + optional exact SHA | source/delivery route | invalid event/SHA/mode |
| 10 SOURCE | exact checkout | 40-char SHA | checked-out HEAD equality | checkout mismatch |
| 20 CAPACITY | executable toolchain | repository source | Java/Android/Gradle report | bootstrap fail |
| 30 TOPOLOGY | one active workflow | `.github/workflows` | single-root + no external JS actions | topology fail |
| 40 QUALITY | source/test capability | Java/C/Gradle | smoke + dependency/freestanding gates | quality fail |
| 50 ARCH | target capability | NDK + C core | ARMv7 ABI + ARM64 undefined-symbol checks | architecture fail |
| 60 EVIDENCE | provenance capability | exact source/run | provenance asset + SHA KAT | evidence fail |
| 70 BUILD | materialization capability | gated source | debug APK | build fail |
| 80 RECEIPT | custody capability | APK | APK hash + entry hash manifest | receipt fail |
| 90 DELIVERY | bounded publication | green gate | prerelease/signing receipt | delivery fail |
| 99 SUMMARY | scoped promotion | executed phases | run summary | never upgrades failed phases |

Promotion rule:

```text
CAPABILITY = SOURCE + EXECUTION + EVIDENCE_WITHIN_SCOPE
CAPABILITY != EXTERNAL_AUDIT
```

Unexecuted capability remains `NOT_RUN` or `TOKEN_VAZIO`; it is never inferred from neighboring green phases.
