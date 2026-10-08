# RAFAELIA — D Coefficients: direct, derivative, antiderivative, inverse, reverse, log, permutation V1

Copyright (c) 2026 Rafael Melo Reis.
SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

## Authority and boundary

This file is an *application-level mathematical crosswalk*, not a successor edit of the formal producer repositories and not a physics measurement.

Canonical formal sources:
- instituto-Rafael/relativity-living-light@53671d2430347f01184fad8a99ca0ced2b0fe2c4, `data/formulas/RAFAELIA_FORMAL_UNIFIED_CORE.md` and `docs/formulas/RAFAELIA_SYMBOL_TABLE.md` (adaptive `D_i,D_0,D_neg`, graph `F_i`, modulation `M_i`, discrete log proxy).
- rafaelmeloreisnovo/Matem-tica-@1e60a9af8cc6500fd4718350023d527f739adc70, `docs/formal/PITAGORAS_BHASKARA_ISOSCELES_POINCARE_CROSSWALK_V1.md` (`D_theta`, `D_Q` and crosswalk).
- rafaelmeloreisnovo/Matem-tica- `papers/2026-07-17_antiderivada_vazio_fluxo_toroidal.md` (derivative + telescoping accumulation; typed void, reversals and permutation invariants).
- rafaelmeloreisnovo/Matem-tica- `docs/formal/FIBONACCI_INVERSE_REVERSE_JUMP_RULER_V1.md` (multivalued exact inverse, log-based estimator, metadata-required inverse jumps).
- rafaelmeloreisnovo/Matem-tica- `docs/formal/SESSION_UNIFIED_TYPED_REALIZATION_V2_2026-09-24.md` (exact antiderivative correction, branch ambiguity and finite permutations).

Invariant: `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`. `TOKEN_VAZIO != 0`. Geometric model coefficients **must not** be silently used as acoustics diffusion coefficients or RF attenuation coefficients.

## D namespace — the sticking point

| Symbol | Typed input | Meaning / scope | Example non-equivalence |
|---|---|---|---|
| `D_i` | numeric graph state/gain per discrete step | adaptive diffusivity of graph update | NOT a measured m²/s diffusion constant |
| `D_0` | nonnegative gain (for inverse >0) | baseline gain and possible entropy feedback | NOT `D(z=0)` used for cosmological growth normalization |
| `D_neg` | strictly negative gain | anti-diffusive branch term | NOT negative measured radio power |
| `D_theta=-2ab cos(theta)` | two lengths and angle | law-of-cosines angular correction, units length² | NOT dimensionless `D_i` |
| `D_Q=-(B²-4AC)/(4A)` | quadratic `Ax²+Bx+C` with A≠0 | quadratic deficit; inherits polynomial units | NOT the geometric distance `D(p,q)` |
| `D(p,q)` | metric specified separately | distance in inverse Fibonacci ruler | NOT a diffusion or response coefficient |

The canonical RLL model is:
`T_i(t+1)=T_i(t) + D_i F_i`;
`F_i=sum_j C_ij (T_j-T_i)`;
`D_i=D0(1-tanh P_i)+Dneg*H(T_i-Theta_i)` with `H(x)=1 iff x>0`, else 0.
A later RLL step uses `D_i <- D_i(1+M_i)` with `M_i=tanh(w0*T_i + w1*F_i + w2*P_i + w3*L_i)`, before the optional separate ascending term `A_i=alpha*abs(F_i)*I[D_i<0]`.

### Why D was the mathematical bottleneck

1. **Discontinuity**: `D` jumps by `Dneg` at `T_i=Theta_i`. A derivative wrt T is not a classical derivative at the threshold. Do not use ordinary finite-difference derivative across it and call it a smooth physical parameter.
2. **Sign**: for D0≥0, `D_i<0` iff the threshold is ON and `abs(Dneg)>D0*(1-tanh P_i)` (ignoring finite-precision saturation and later M). Baseline diffusion no longer supplies a general stability guarantee.
3. **Coupling**: if `M_i` or `P_i` depends on `T`, then a derivative of the entire recurrence needs the chain rule and the neighbor Jacobian, not just `partial D/partial P`.
4. **Units**: `D_i` is an effective per-step gain in this model, because no lattice spacing, step duration, material diffusion constant or spatial calibration is specified. Do not infer physical diffusion coefficient or measured echo distance.
5. **Identification**: `D_0,Dneg,P,Theta,M` are not identifiable from the current mono microphone RMS/six Goertzel bands, GPS/Wi-Fi cached levels or accelerometer alone. Their inferred physical values remain `TOKEN_VAZIO_UNIDENTIFIED`.

### Derivative, antiderivative, inverse (fixed branch only)

For fixed `T,Theta,D0,Dneg`, let `h=H(T-Theta)`.

`D(P)=D0*(1-tanh P)+Dneg*h`.

- Direct: `D(P)`.
- Derivative: `dD/dP=-D0*sech²(P)`; derivative wrt T is zero away from threshold only if P,Theta fixed.
- Primitive wrt P: `G(P)=D0*(P-ln cosh(P))+Dneg*h*P+C`. Differentiate `G` to check.
- Inverse fixed branch, `D0>0`: `P=artanh(1-(D-Dneg*h)/D0)`, only if the inner term is strictly within `(-1,1)`. If `h` unknown this is a *set of branch candidates*, not a globally invertible function.
- Modulation: `D_eff=D*(1+M)`. If M known and `-1<M<1`, `D=D_eff/(1+M)`. If M unknown or jointly variable: `TOKEN_VAZIO_COUPLED_INVERSION`.
- With `M=tanh(w0*T+w1*F+w2*P+w3*L)` and other inputs fixed, derivative `dD_eff/dP=(1+M)*(-D0*sech² P)+D*w2*(1-M²)`; code only implements the partial unmodulated derivative and known-M inverse.

### Discrete RLL logarithmic derivative is not exact log increment

The RLL source calls `L=(T_t-T_(t-1))/(T_(t-1)+eps)` a discrete equivalent of a log derivative. It is the *first-order ratio proxy*, whereas the actual log difference is:

`Delta_ln = log(T_t+eps)-log(T_(t-1)+eps) = log1p(L)`.

Conditions: `eps>0` and `T_t+eps>0`, `T_(t-1)+eps>0`; positive epsilon alone is **not** sufficient for signed T. Reverse exact log step: `T_t=(T_(t-1)+eps)*exp(Delta_ln)-eps`. The proxy `L` and the exact increment coincide approximately only for `abs(L)<<1`.

As a controlled algebra example (NOT measured phone data), previous=1, current=2, eps=0.01: `L≈0.9901` but `Delta_ln≈0.6882`. Treating proxy as exact log can bias a phase/coherence or feedback model.

For a signed coefficient D (which may be negative), `ln(D)` is undefined in real numbers when D≤0. A distinct, *explicitly declared model transform* is `g(D;s)=sign(D) ln(1+abs(D)/s)` for s>0, with inverse `g^-1(y;s)=sign(y)*s*(exp(abs(y))-1)`. It is a dimensionless, sign-preserving visualization/compression transform, not a physical logarithm law.

A double logarithm `log(log(x/s))` needs `x/s>1`; negative D is not admissible. Do not confuse `loglog` with `log1p`, or normalize dimensionful D without a reference scale.

### Other coefficients and direct/inverse branches

For `D_theta=-2ab cos(theta)`:
- `dD_theta/dtheta=2ab sin(theta)`;
- primitive `-2ab sin(theta)+C`;
- inverse `theta=±acos(-D_theta/(2ab))+2*pi*k` (a,b>0 and cosine argument ∈[-1,1]). A principal arccos branch is **not** the complete angular inverse.

For `D_Q=C-B²/(4A)` (`A!=0`), at fixed A and C:
- `partial D_Q/partial B=-B/(2A)`;
- primitive wrt B: `C*B-B³/(12A)+constant`;
- inverse in B: `B=±sqrt(4A(C-D_Q))` if radicand≥0. Sign ambiguity remains even with an exact D_Q measurement.

### Reverse vs inverse vs permutation

Discrete derivative `Delta X_k=X_(k+1)-X_k`; discrete antiderivative `X_n=X_0+sum Delta X_k` requires a known initial state. Losing the anchor means the inverse derivative has a free constant. Chronological reverse lookup is a graph-predecessor query, not a unique functional inverse.

For a two-node graph with gains `dL,dR`, forward operator is:
`T'=[[1-dL,dL],[dR,1-dR]] T`.
Its determinant is `1-dL-dR`, so algebraic inverse is singular at `dL+dR=1`. Even if invertible, reversing a computation does not reverse dissipative physics or reconstruct measurement noise that was never stored.

A permutation `P` of node order is only a representation symmetry when states, coefficients, AND adjacency/geometry transform consistently: `T'=P T`, `D'=P D`, `A'=P A P^-1`. Permuting only samples is not a substitute for physical movement of sensors or a phase/echo measurement. A permutation preserves an unweighted sum of squares of a fixed vector, but not its time ordering.

### Stability and explicit anti-diffusion

For a six-neighbor nonnegative weighted averaging step `T_i'=T_i+D sum_j C_ij (T_j-T_i)` with C≤1, `0≤D≤1/6` is a sufficient local convex-combination bound. It is NOT a universal necessary/sufficient condition for time-dependent coupled RLL, nonsymmetric signed graph operators, or physical acoustic propagation.

For two nodes, the synthetic difference evolves as `delta'=(1-2D)*delta`. With `D=0.25`, contrast shrinks; with `D=-0.25`, contrast grows. Negative D is an anti-diffusive regime; the additional RLL `alpha|F|` source term requires a separate boundedness and stability study.

`D0 <- D0*(1+gamma*S_H)` can change stability from one time step to the next; a single initial D check is not sufficient to claim a stable full adaptive model.

### Heuristic route, clearly marked NOT physical proof

1. **Derivative route**: evaluate sensitivity of synthetic D wrt P, T (excluding discontinuities), M, and uncertainty propagation.
2. **Antiderivative route**: reconstruct discrete time series with an anchor; compare any continuous primitive derivative to original coefficient.
3. **Inverse/reverse route**: identify branch multiplicity, determinant-zero cases and lost metadata; preserve typed uncertainty rather than inventing a unique predecessor.
4. **Log route**: test ratio-proxy error as a function of fractional change; use signed log only after explicit reference scale.
5. **Permutation route**: ensure relabeling transports state/gain/adjacency. Compare against invalid state-only permutation as negative control.
6. **Physical source route**: no coefficients D promoted from GPS, Wi-Fi, BLE, modem or PCM without an independent calibrated observable and a fitted falsifiable model.

## Implemented and proof scope

- `app/src/main/java/io/rafaelia/audiostudio/WaveLabDOperators.java` is Java numeric operator code with no Android imports (JVM/ART hosted, **NOT freestanding C**). It is separate from Wi-Fi/Bluetooth/cellular/GNSS observation code.
- `ci/WaveLabDOperatorsTest.java` covers D branch/sign/threshold, finite derivative vs central difference, analytic primitive, conditional inverse, known-M inverse, log proxy vs exact and reverse, typed invalid domains, telescoping, bijective permutations, geometric D_theta, quadratic D_Q, negative diffusion, graph coefficient-permutation covariance and singular dynamic inverse.
- `ci/wave-lab-gate.sh` runs these fixtures with javac alongside existing WaveLabCoreTest on the fast lane.
- No physical D parameters, radiation measurements, actual echo DOA, SPL, or radio control are added. No RF setters, scan triggers or hardware states touched by this work.
- Evidence statuses must come from exact-head GitHub CI log; `IMPLEMENTED_UNTESTED` until executed. A successful fast gate is **not** Android UI compile or physical experiment success.

## R3## Successor implementation — bounded W7 synthetic experiment (2026-10-08)

- Producer [WaveLabDSimulator.java](../../app/src/main/java/io/rafaelia/audiostudio/WaveLabDSimulator.java) uses a **7-node undirected wheel** (hub degree 6, ring degree 3); this is not a full hexagonal lattice or an Android sensor model.
- Positive branch: D0=0.10, Dneg=-0.20, P=0, H=0 gives D=+0.10. Negative branch: same coefficients, H=1 gives D=-0.10. Threshold is held fixed per run; no plasticity, feedback, source term or M modulation yet.
- Deterministic guardrails: maximum 32 update steps and max observed absolute node value 8. A STOP_MAGNITUDE_BOUND status is a **bounded simulation stop**, not a discovery.
- The [WaveLabDSimulatorTest.java](../../ci/WaveLabDSimulatorTest.java) host fixture checks symmetric mass conservation, variation trends, stop, invalid inputs, deterministic report and absence of raw sensor identities.
- UI button: **Experimento D · simular (sem sensores / radio)**; generated text is embedded in the existing user-triggered ZIPRAF observations; initial state NOT_RUN_SYNTHETIC. Nothing uploads automatically.
- Related Papers study: https://github.com/rafaelmeloreisnovo/papers/blob/research/wave-lab-d-coefficients-20261008/research_notes/2026-10-08_D_COEFFICIENTS_WAVE_LAB_BRIDGE_V1.md; RLL index: https://github.com/rafaelmeloreisnovo/relativity-living-light/blob/docs/rll-d-coefficients-navigation-20261008/docs/science/RLL_D_COEFFICIENTS_WAVE_LAB_INDEX_V1.md.
- Current state: latest exact-head CI evidence must be checked; fast-lane success != Android build/device execution; claim_allowed=false.



F_ok = typed symbols/derivatives/inverses with explicit domain, finite numerical fixtures and falsifiers.
F_gap = source-producer correction agreement, full coupled simulation, on-device measurement, calibrated D identification and global stability.
F_next = exact-head host CI → independent review → full Android build → only then parameter-fitting tests with calibrated actual data; no radio mutation.
claim_allowed=false.
