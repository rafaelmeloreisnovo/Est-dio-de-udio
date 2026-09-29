# Multimodal Signal Architecture V1

Authority bridge: `ZIPRAF_OMEGA_FULL/ativar.txt` canonical V2.

## Invariant

```text
VISION != ARTIFACT != IMPLEMENTATION != EXECUTION != EVIDENCE != CLAIM
symbol != content
measured != simulated != estimated != extrapolated
```

## Canonical route

```text
SOURCE
-> OBSERVATION
-> NORMALIZATION
-> VECTOR/MATRIX
-> LAYERS
-> CODEBOOK/INDEX
-> PROVENANCE/PARITY
-> RECONSTRUCTION
-> RECEIPT
```

## State vector

A multimodal observation may be represented as:

```text
X[n] = {
  audio,
  acceleration,
  magnetic_field,
  orientation,
  time,
  device,
  room,
  connectivity_metadata,
  provenance
}
```

The fields retain their units and epistemic states. They are not directly summed.

## Observation families

```text
audio          -> PCM / Δp-derived digital signal
accelerometer  -> Δa
magnetometer   -> ΔB
orientation    -> Δorientation
connectivity   -> passive Android transport metadata only
```

Correlation among those families is not a causal claim.

## Representations

Toroidal, Poincare, vector, matrix and graph views are analytical coordinate systems unless a separately validated physical model is supplied.

```text
representation != physical mechanism
```

## Reconstruction

A reversible claim requires:

```text
encode -> damage/ablation where applicable -> decode -> byte/sample/hash comparison
```

RAC1 v1 currently targets exact PCM16 sample reconstruction.
