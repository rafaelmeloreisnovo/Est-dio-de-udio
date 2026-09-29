# Verification and Evidence

## Five distinct layers

```text
SOURCE -> ARTIFACT -> EXECUTION -> EVIDENCE -> CLAIM
```

Each arrow requires an explicit transformation.

## Examples

- C file exists: SOURCE.
- APK built: ARTIFACT.
- workflow ran: EXECUTION.
- ARMv7 zero-undefined step passed: EVIDENCE.
- “this APK's ARMv7 core has zero undefined symbols under the recorded toolchain”: bounded CLAIM.

A physical CFR is evidence for the measured speaker-room-microphone path.
It is not by itself evidence of absolute SPL, RT60 or a corrected room.

## Installed evidence bundle

The app hashes its installed APK bytes and records package metadata. If built through the canonical CI, the embedded provenance asset identifies the source SHA and native gates that passed before assembly.

## Integrity

SHA-256 binds bytes, not meaning.
A receipt binds an execution statement to references.
Neither prevents all tampering nor proves scientific validity without the test protocol.
