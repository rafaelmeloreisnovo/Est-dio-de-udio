# Multi-ABI physical user-observed receipt — 6666ce0e — 2026-10-02

schema=rafaelia.multi-abi-physical-user-observed/v1

## SOURCE / ARTIFACT binding

- repo: `rafaelmeloreisnovo/Est-dio-de-udio`
- immutable release tag: `rafaelia-live-36999101130`
- release source SHA: `6666ce0ee0b9aa0ef118ed89a215d7cdab501ebc`
- release run ID: `36999101130`
- release asset: `rafaelia-audio-studio-6666ce0ee0b9.apk`
- release asset SHA-256: `20d31c49c361a50f87b28c140e8a2cf98cf8e70282d17127e743649809b9e3dd`
- user-uploaded APK SHA-256: `20d31c49c361a50f87b28c140e8a2cf98cf8e70282d17127e743649809b9e3dd`
- exact-byte binding: `UPLOADED_APK_SHA256 == RELEASE_ASSET_SHA256 = PASS`

## Direct inspection of the uploaded APK

The uploaded APK bytes were inspected directly before this receipt update.

- ZIP container integrity: `PASS`
- packaged native libraries: `2`
- `lib/armeabi-v7a/librafaelia_audio.so`: `ELF32`, ARM, EABI5, Android 29 target metadata
- `lib/arm64-v8a/librafaelia_audio.so`: `ELF64`, AArch64, Android 29 target metadata
- `MULTI_ABI_PACKAGE_PRESENT=PASS`

This proves that the exact uploaded/release APK physically contains both ABI payloads. It does not by itself prove which ABI Android selected at runtime on either device.

## Physical execution — user attestation

The user reports that this exact multi-ABI APK installed/ran smoothly on both physical devices below:

1. POCO device
   - exact model: `TOKEN_VAZIO`
   - runtime result: `PASS_USER_ATTESTED_SCOPED`
   - crash observed/reported: `NO_REPORTED_CRASH`
   - selected runtime ABI: `TOKEN_VAZIO_NOT_INSTRUMENTED`

2. Motorola moto e(7) power
   - runtime result: `PASS_USER_ATTESTED_SCOPED`
   - crash observed/reported: `NO_REPORTED_CRASH`
   - selected runtime ABI: `TOKEN_VAZIO_NOT_INSTRUMENTED`

## Evidence boundary

This receipt records direct artifact inspection plus a real-world user observation against an exact hash-bound release artifact. It does **not** promote that observation into an instrumented ABI-selection proof or a formal automated device-test PASS.

- `PHYSICAL_DEVICE_COUNT_USER_ATTESTED=2`
- `PHYSICAL_RUNTIME_USER_ATTESTED=PASS_SCOPED`
- `FORMAL_INSTRUMENTED_DEVICE_TEST=NOT_RUN`
- `ABI_SELECTED_AT_RUNTIME=TOKEN_VAZIO`
- `ADB_GETPROP_RECEIPT=TOKEN_VAZIO`
- `INSTALLED_BASE_APK_SHA_READBACK=TOKEN_VAZIO`
- `FORMAL_FUNCTIONAL_TEST_MATRIX=NOT_RUN`
- `CLAIM_ALLOWED=false`

Boundary:

`MULTI_ABI_PACKAGE_PRESENT != RUNTIME_ABI_SELECTION_PROVEN`

`USER_ATTESTED_RUNTIME_PASS != FORMAL_INSTRUMENTED_DEVICE_TEST_PASS`

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`

## Next smallest evidence-producing step

On each device, capture only the minimal deterministic receipt needed to resolve the remaining ABI ambiguity:

```text
getprop ro.product.model
getprop ro.product.cpu.abi
getprop ro.product.cpu.abilist
package=io.rafaelia.audiostudio
installed_base_apk_sha256=<readback>
release_apk_sha256=20d31c49c361a50f87b28c140e8a2cf98cf8e70282d17127e743649809b9e3dd
```

Then bind the selected/runtime ABI separately for POCO and moto e(7) power without changing the historical observation above.

R3=<F_ok:exact release bytes identified + direct multi-ABI inspection PASS + physical runtime observed on 2 devices,F_gap:selected runtime ABI + installed-byte readback + formal functional test,F_next:capture minimal per-device ABI/hash receipt>
