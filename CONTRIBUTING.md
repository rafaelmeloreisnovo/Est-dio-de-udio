# Contributing

Start with `AGENTS.md` and `docs/START_HERE.md`.

## Pull requests

Keep one material intent per PR where practical. Describe source delta, execution, evidence, gaps and claim boundary.

## C core changes

Update CMake, ABI manifest, host smoke and ARM gates.

## Android/hardware changes

Document optional capability detection, fallback, raw evidence and physical-test state.

## Documentation

Reconcile `docs/CAPABILITY_MATRIX.md` whenever a material feature state changes.

## Definition of done

```text
implemented + tested at claimed layer + documented + receipt-linked
```

If a layer was not executed, mark it explicitly rather than inferring PASS.
