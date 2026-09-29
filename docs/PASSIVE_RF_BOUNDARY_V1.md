# Passive RF Boundary V1

## Purpose

Keep radio-related observation inside the operating system's existing, homologated device behavior.

## Allowed project surface

The app may read passive metadata exposed through ordinary Android APIs, for example:

- whether the device declares telephony capability;
- the transport class of the currently active network such as CELLULAR/WIFI/ETHERNET/VPN;
- availability/unavailability states.

## Explicitly outside this project path

```text
NO RF transmission generation
NO spectrum injection
NO modem/baseband modification
NO transmit-power control
NO frequency/band override
NO firmware patching
NO bypass of device certification or regulatory limits
NO active spectrum experimentation
```

The implementation `PassiveRadioObservation` queries platform metadata only.

## Regulatory boundary

Brazilian telecommunications equipment and radiofrequency-emitting equipment are subject to the applicable Anatel certification/homologation framework. This software contract deliberately does not attempt to alter the certified RF behavior of the device.

```text
PASSIVE_METADATA != RF_MEASUREMENT_INSTRUMENT
PASSIVE_METADATA != TRANSMITTER_CONTROL
```

This document is an engineering boundary, not legal advice.
