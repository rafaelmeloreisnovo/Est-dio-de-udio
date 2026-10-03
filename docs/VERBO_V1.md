# VERBO V1 — Estúdio de Áudio

VERBO is the small deterministic governance core that sits between an observed operation and a promoted claim.

It does not process audio and it does not manufacture evidence. Its job is to preserve the boundary:

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

and the state rules:

`TOKEN_VAZIO != 0`

`IMPLEMENTED_UNTESTED != PASS`

## Three planes

- BODY: execution/evidence state.
- SOUL: source, authority and provenance state.
- SPIRIT: computational-ethics constraints.

SPIRIT includes explicit privacy, dignity, child-safety, human oversight and reversibility gates. A missing gate blocks promotion rather than being inferred.

## Audio-specific meaning

A recording, DSP output, meter reading or exported container is an artifact. It is not automatically proof that a specific execution path ran. A physical-device receipt can support an execution claim only for the device/build/path actually observed.

Microphone/audio content remains user data. VERBO therefore treats privacy and human control as execution constraints, not decorative documentation.

## Current state

The source files are implemented on a feature branch. Until compilation/tests on the exact head exist, state remains `IMPLEMENTED_UNTESTED` and no PASS claim is authorized.
