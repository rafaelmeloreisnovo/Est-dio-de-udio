# Gap / TOKEN_VAZIO Ledger V1

## Rule

A missing implementation may be filled.
A missing measurement may only be filled by measurement.

| Item | State | Action |
|---|---|---|
| compact adaptive UI | IMPLEMENTED_UNTESTED | CI then physical screen test |
| narrow ARM APK | IMPLEMENTED_UNTESTED | ARMv7 + AArch64 only |
| R8 in canonical path | REMOVED_SOURCE | CI verify |
| resource shrink | REMOVED_SOURCE | CI verify |
| third-party Java runtime deps | NONE_DECLARED | preserve |
| C core libc/libm/heap | GATED_ZERO | preserve |
| JNI bridge | PRESENT_REQUIRED_BY_CURRENT_ARCH | migration, not deletion |
| Android SDK build dependency | PRESENT_BUILD_TIME | cannot remove from current Gradle APK path |
| Gradle build dependency | PRESENT_BUILD_TIME | future direct toolchain route possible |
| absolute SPL | TOKEN_VAZIO | physical acoustic reference required |
| measured response curve | TOKEN_VAZIO | execute CFR/analysis |
| physical μ∆ baseline | TOKEN_VAZIO | execute on target device |
| hardware fault model | TOKEN_VAZIO | validated dataset/model required |
| physical Android 10 UI fit | NOT_RUN | install and inspect |

## Protected empties

`ABS_SPL`, physical vibration baseline and hardware diagnosis must not be populated from code constants, guesses or simulated values.
