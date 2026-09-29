# FAQ

## Is the entire APK freestanding?

No. Android services are required for UI, audio, storage, sensors and codecs. The portable DSP/measurement cores are freestanding.

## Does the CAL sweep measure absolute dB SPL?

Not without a physical acoustic reference. Absolute SPL remains `TOKEN_VAZIO`.

## Does μ∆ detect a broken motor or bearing?

It observes short-term acceleration changes. Fault diagnosis requires a validated model, reference conditions and evidence.

## What does the evidence button prove?

It records the installed APK hash, package/install metadata, available CI provenance, device/runtime facts, sensor inventory, μ∆ observation and available artifact hashes.

## Can a PASS receipt prove every feature?

No. PASS belongs only to the executed gate named in the receipt.

## Why preserve raw CFR/ZRF data?

So later algorithms can be rerun without fabricating or losing the original measurement.
