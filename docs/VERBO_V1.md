# VERBO V1 — Estúdio de Áudio

VERBO is a deterministic governance core between observed operations and promoted claims. It does not process audio or manufacture evidence.

It preserves:

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

`TOKEN_VAZIO != 0`

`IMPLEMENTED_UNTESTED != PASS`

## Planes and required gates

- **BODY** combines artifact, execution and evidence state.
- **SOUL** requires source and authority.
- **SPIRIT** requires consent, privacy, dignity, child safety, human oversight and reversibility.

Each required input is a presence gate. A zero value sets its explicit `gap_mask` bit and blocks `claim_allowed`. In particular, an artifact cannot be inferred from execution/evidence, and consent cannot be inferred from privacy or authority.

A recording, DSP output, meter reading or exported container is an artifact. It is not by itself proof that a specific execution path ran. A physical-device receipt supports a claim only for the device, build and path actually observed.

## Build and integration boundary

`rfa_verbo_core` is built as a freestanding static C module. The exact-head CI gate must run the host smoke vectors and ARMv7/AArch64 zero-undefined plus ABI-manifest checks.

The core is not yet called by the JNI or audio execution path. CI evidence for this module therefore does not establish app-level enforcement, physical behavior, microphone behavior or an audio-quality claim. Those remain separate gates; `claim_allowed=false` until each relevant domain contract is satisfied.
