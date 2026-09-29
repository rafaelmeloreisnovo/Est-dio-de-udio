# μ∆ Vibration V1

## Definition

Here `μ∆` means **micro-delta step**: a local temporal difference, not the SI prefix micro.

For accelerometer vector `a[k]=(x,y,z)`:

```text
Δa[k] = a[k] - a[k-1]
D²[k] = Δx² + Δy² + Δz²
RMS_Δ = sqrt(mean(D²))
PEAK_Δ = sqrt(max(D²))
```

Units are m/s² because the difference is between acceleration samples.

## Why delta

Static gravity and constant orientation dominate raw accelerometer magnitude. Temporal differences suppress a constant offset and emphasize change, including vibration and motion.

## What is stored

- sensor name/vendor/version;
- sample count;
- first/last sensor timestamp;
- effective sampling rate;
- RMS and peak `Δa`;
- min/max raw axes.

## Limits

This is not a calibrated vibrometer standard.
Sampling can be irregular.
Phone mounting, sensor range, Android scheduling and orientation affect results.
Frequency-domain vibration analysis is a future separately gated layer.
