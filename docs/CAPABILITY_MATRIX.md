# Capability Matrix

| Capability | Source | CI/build | Physical | Claim state |
|---|---|---|---|---|
| PCM capture | implemented | build-gated | device-dependent | bounded |
| DSP/meter freestanding cores | implemented | ARMv7/AArch64 gated | N/A | bounded |
| ZRF session container | implemented | build-gated | available | bounded |
| CFR relative capture | implemented | build-gated | requires run | physical result NOT_RUN until CFR exists |
| absolute SPL | schema only | N/A | reference required | TOKEN_VAZIO |
| IR/deconvolution | partial primitives | pending integration | NOT_RUN | PENDING |
| RT metrics | UI/contract route | not complete | NOT_RUN | PENDING |
| room correction | FIR primitive + route | not complete | NOT_RUN | PENDING |
| voice acoustic features | implemented reference | gated | input-dependent | bounded |
| pitch-preserving autotune | not complete | NOT_RUN | NOT_RUN | PENDING |
| accelerometer μ∆ vibration | implemented platform probe | build-gated | generated on device | OBSERVED_UNPROMOTED |
| hardware fault diagnosis | not implemented | N/A | reference/model absent | NOT_CLAIMED |
| installation evidence bundle | implemented | build-gated | generated on device | bounded |

Update this matrix whenever a receipt materially promotes or demotes a state.
