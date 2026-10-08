# RAFAELIA | Wave Physics Lab alpha1 — START HERE
Copyright (c) 2026 Rafael Melo Reis. SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1

**Production boundary:** this is a DRAFT implementation on `feature/wave-physics-lab-readonly-20261008`. Requires exact-head CI, signed-release review, on-device test and permission UX verification. Not yet an installed APK feature.

## P0 invariant: absolutely no radio configuration changes
Allowed: read-only `WifiManager.getScanResults()` (cache only, no `startScan`), `LocationManager.getLastKnownLocation(GPS_PROVIDER)`, `TelephonyManager.getAllCellInfo()`, `BluetoothAdapter.isEnabled()`, local `SensorManager` events. GPS precision/location service remains platform-controlled. NO start/stop scans, no Wi-Fi/Bluetooth toggling, no channel/frequency/tx-power/APN/modem changes, no RF transmit or injection. Permission denial -> typed TOKEN_VAZIO. Bluetooth LE active scanning, GNSS callbacks, cellular CSI, RF IQ and camera pixel capture are NOT IMPLEMENTED in alpha1. User-approved scans could be a future separately gated action using ordinary Android APIs; never change radio policy to force scan availability.

## Actual alpha1 UI
- Advanced tools -> "Wave physics lab (read-only)" -> native Android Activity, independent of existing audio flows.
- Accelerometer xyz, delta-x RMS and orientation via TYPE_ROTATION_VECTOR when present; eight-sector **device pose**, never a direction-of-arrival claim.
- Single 4096-sample/48k mono PCM microphone window only after explicit action and RECORD_AUDIO permission; six Goertzel bins at 125,250,500,1000,2000,4000 Hz, approximate uncalibrated relative dBFS. Not a full spectral FFT, not SPL, not spatial audio.
- Explicit snapshot aggregates a recent GPS last fix or typed stale gap (age <=120 s), Wi-Fi *cached* access-point count / strongest RSSI and channel frequency with cache-age gap, cellular aggregate signal, Bluetooth adapter state. BSSID, MAC, SSID and cell IDs not exported. Exact latitude/longitude may appear only after user consent; treat ZIP as sensitive. No GPS fix or Android permission -> TOKEN_VAZIO.
- Existing **relative** calibration result may be forwarded from MainActivity as a provenance-limited readout; no new acoustic excitation is triggered by the laboratory. Echo direction and calibrated range remain TOKEN_VAZIO.
- Explicit ZIPRAF export in Downloads/RafaeliaAudio/Physics using existing project-owned `RfaStoredZip`. Contains manifest and plaintext observation only; CRC32 is corruption detection, not cryptographic authenticity. No audio samples, raw radio identifiers, photos, or IQ are embedded.

## Source-bound formula routes (NOT all 609 formulas automatically accepted)
- MCM:MEM:DELTA_A:017:v1: discrete difference and telescoping accumulation -> accelerometer delta/time series.
- MCM:TEO:PHASE_SCALE:012:v1: polar/phase coordinate projection -> bounded analytic visualization. Angle branches matter.
- MCM:TEO:SPIRAL_R:003:v1: geometric recurrence sqrt(3)/2 -> **model comparison only**, not a measured physical decay law.
- Mathematical Poincare SECTION: sectionCrossing(prev_x, curr_x) counts rising x=0 crossings; counts alone do not establish a return map, recurrence, chaos, torus stability or theorems.
- Haversine geodesic: only if two geodetic positions are fresh and the accuracy radius is declared. Single GPS fix gives no baseline.
- Echo planar toy model d = c*(t_echo - t_io)/2. Both speed-of-sound with physical temperature and device I/O latency need independent estimates: `range_m=TOKEN_VAZIO` without them.
- Triangle distance / Pythagoras: calibrated coordinate system needed. Bhaskara quadratic roots only when model coefficients and discriminant domain established.
- Venturi/Bernoulli: pressure, density, area/flow sensors missing; NOT MEASURED from wifi signal or sound pressure. Toroid/√3/2 are geometry/model seeds, not RF-field observations.
- Steinberg VST3 Ambisonic arrangements (ACN/SN3D): FOA=4 channels, second=9, third=16; a mono capture is **not** ambisonic and cannot uniquely infer bearing.

## Falsification / gates
- P0 code scan: reject WifiManager.startScan, setWifiEnabled, BluetoothLeScanner.startScan, BluetoothAdapter.enable/disable, TelephonyManager setters, LocationManager mutation, reflection/exec/hidden APIs, `CHANGE_WIFI_STATE`, `BLUETOOTH_ADMIN`, `CAMERA` permissions.
- Missing sensors on Moto e(7) power: magnetometer absence is TOKEN_VAZIO_NOT_PRESENT, not 0 measurement. No physical runtime pass until a real Android receipt.
- If radio reading is stale, denied or unsupported, do not interpolate as measured. No whole-application freestanding claim; Android platform layer is hosted, `WaveLabCore` is platform-free Java (still ART).
- Exact-head compile/CI/build/device runtime and permission tests: NOT_RUN until observed. Static source review is not execution proof.
- Data retention: Android download export remains on user device; uploading to Drive requires deliberate user action. Do not upload GPS identifiers automatically.
- Rollback: close draft PR; no main changes, no external permission/provider state changed.

R3=<F_ok: isolated pilot code and explicit P0 policy, F_gap: real device/CI + directional echo, BLE/GNSS-radio features and 609 formula crosswalk, F_next: Android compile + physical consent/differential test + typed ZIPRAF readback>.
