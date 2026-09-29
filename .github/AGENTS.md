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
-> upload artifact
```

Do not place a PASS statement in the embedded provenance for a later step that has not executed yet.

Workflow syntax changes are material: malformed YAML can suppress all evidence. Prefer complete canonical reconstruction over repeated partial text substitution when workflow structure is damaged.

Preserve FAIL history in receipts when it caused a corrective change.
