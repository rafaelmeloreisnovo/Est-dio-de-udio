# UI Surface Decisions V3 — 2026-10-02

## Intent
Reduce visible friction and keep the primary Android surface limited to controls and states that are executable or materially informative.

## Source
- base: `main@e5b20631eca7259c133a09d68d5c606a9ea10a45`
- policy: `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`
- policy: `TOKEN_VAZIO != 0`
- policy: `IMPLEMENTED_UNTESTED != PASS`

## Removed from the always-visible surface

### Duplicate transport row
Removed from the screen because `StudioWorkspaceView` already owns the executable `REC`, `STOP/MASTER` and `PLAY` transport actions.

### Duplicate evidence row
Removed from the always-visible screen because `PROOF` / ZIPRAF generation already exists in the workspace. Evidence sharing remains available under the compact `MORE` panel.

### Duplicate `SpectrumView`
Removed from the activity surface. The active 16-band spectrum remains in `StudioWorkspaceView > MEASURE`, where it is tied to the current mastering result.

### Always-visible teleprompter controls
Moved behind the explicit `NARRATION` control. Narration and teleprompter behavior remain implemented; they no longer occupy the primary measurement surface when unused.

### Always-visible interoperability/export controls
Moved behind the explicit `MORE` control. RAW PCM, WAV PCM16, processed-audio sharing, ZIPRAF sharing, playback stop and pre-flight remain executable.

### `OPUS / SHARE` duplicate
Removed. It duplicated the existing processed-audio share action.

### Static explanatory footer
Removed from the main screen because custody/evidence policy is already rendered in the `EVIDENCE` section and does not need a second permanent block.

### Optional sensor-access action
Removed from the primary workspace. In the current Android profile, accelerometer/magnetometer/light/proximity availability is a runtime/hardware state and does not require an invented permission action. Physical sensor observation remains part of explicit evidence generation.

## Claim cleanup
The UI no longer presents profile labels such as `-16 LUFS`, `-18 LUFS` or `EBU R128` as user-facing measurement claims. The underlying preset/target-energy routing remains unchanged. External normative audit remains `NOT_AUDITED`.

## Clickable visual surfaces
Dashboard cards and waveform are navigation targets:
- SIGNAL / waveform → SESSION
- INPUT → SYSTEM
- CALIBRATION → MEASURE
- EVIDENCE → EVIDENCE

Measurement charts remain visible. `PROOF` and `RUN RELATIVE CAL` remain explicit effectful actions rather than being triggered by an ambiguous chart tap.

## Restore criteria
A removed primary control should return only when it is both:
1. materially useful in the dominant workflow; and
2. backed by an executable action or observed state that can be tested.

Decorative placeholders, unexecuted claims and duplicate actions do not return to the main surface.

## Gate state at write time
- SOURCE_REVIEW=PASS
- IMPLEMENTATION=IN_PROGRESS
- JAVA_COMPILE=NOT_RUN
- APK_BUILD=NOT_RUN
- PHYSICAL_UI_REVIEW=NOT_RUN
- CLAIM_ALLOWED=false
