# Hardware Test Protocol

## Scope

Repeatable, low-risk device checks for audio I/O and motion sensors.

## Preflight

- sufficient battery;
- no active call;
- stable phone placement;
- microphone/speaker unobstructed;
- avoid headphones/in-ear devices for acoustic sweep;
- use conservative playback level.

## Audio chain

1. confirm microphone permission;
2. record short raw PCM;
3. inspect clipping/peak/RMS;
4. run relative CAL;
5. preserve CFR;
6. record input source and fallback;
7. compare repeated captures before interpreting drift.

## Accelerometer μ∆

1. place device stationary on a stable surface;
2. tap **Gerar provas + teste μ∆**;
3. preserve sample count, effective rate, RMS Δa and peak Δa;
4. repeat with the intended machine/surface if vibration characterization is desired;
5. compare like-for-like mounting and sampling conditions.

## Claim boundary

A phone accelerometer can observe motion/vibration within its own sensor limits. Without a validated mechanical reference/model, it must not declare a component defective or structurally unsafe.
