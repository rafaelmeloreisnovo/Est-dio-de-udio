# AGENTS.md — .github/

GitHub Actions is an execution/evidence layer.

## Gate ordering

```text
checkout
-> toolchain
-> host smokes
-> freestanding source gate
-> ARMv7 zero-undefined + ABI
-> AArch64 zero-undefined
-> embed provenance
-> assemble APK
-> binary origin receipt
-> optional GH CLI prerelease publish
```

Do not place a PASS statement in the embedded provenance for a later step that has not executed yet.

Workflow syntax changes are material: malformed YAML can suppress all evidence. Prefer complete canonical reconstruction over repeated partial text substitution when workflow structure is damaged.

Preserve FAIL history in receipts when it caused a corrective change.


## Additional low gates

The workflow must retain:

- Low dependency boundary gate;
- zero AndroidX/Kotlin/third-party Java runtime declarations;
- R8/shrink disabled;
- exactly one JNI implementation file at `jni_bridge.c`;
- project-local `LowSha256` known-answer test before APK assembly.


## External Action boundary

Canonical workflows require:

```text
external JavaScript Actions = zero
local reusable workflows = allowed
GitHub CLI = external platform edge
```

Real signing must fail closed when the expected certificate fingerprint or private signing Secrets are absent.
