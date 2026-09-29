# ADAPTIVE_STUDIO_UI_V1

## Design target

A professional workstation surface optimized for phone portrait, phone landscape and larger displays without separate screen implementations.

## Compact layout

```text
TOP TABS
STATUS / TIMECODE
CENTRAL WORKSPACE
PERSISTENT TRANSPORT
REC | STOP | PLAY | CAL | PROOF
```

Detailed controls remain below the console for advanced workflows.

## Wide layout

```text
┌────────┬──────────────────────────────┬───────────────┐
│ rail   │ central workspace            │ inspector     │
│ REC    │ timeline / waveform / CAL    │ source        │
│ EDIT   │ spectrum / room / voice      │ peak / RMS    │
│ CAL    │ master / export              │ clip          │
│ ...    │                              │ CAL/MASTER    │
└────────┴──────────────────────────────┴───────────────┘
┌─────────────────────────────────────────────────────┐
│ REC | STOP | PLAY | CAL | PROOF                     │
└─────────────────────────────────────────────────────┘
```

The layout is recalculated from current view width on each draw. It does not depend on a fixed phone model.

## Primary actions

- REC: start ordinary raw capture;
- STOP: stop and run current mastering path;
- PLAY: play latest master;
- CAL: relative speaker-room-microphone capture;
- PROOF: installation/hardware/μ∆ evidence bundle.

## State semantics

The console no longer uses an empty token where a lifecycle reason is known:

```text
INPUT_IDLE
INPUT_UNAVAILABLE
UNAVAILABLE_NOT_REPORTED
PENDING_PHYSICAL_REFERENCE
NOT_RUN
PENDING
```

A genuinely undefined concept remains `TOKEN_VAZIO`.
