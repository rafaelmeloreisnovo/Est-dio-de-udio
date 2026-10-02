# Installation Validation

## Goal

Answer: **which exact application bytes are installed on this phone, and did Android Package Manager actually accept and launch them?**

## Static CI gate

`ci/apk-installability-static.sh` validates the exact APK without a device:

```text
ZIP integrity
Android signature structure
zipalign
package id
minSdk / targetSdk
armeabi-v7a + arm64-v8a payloads
```

This may produce `INSTALLABLE_STATIC=PASS`. It is not a physical installation claim.

## Real-device ADB gate

With one authorized Android device attached, run:

```text
bash ci/android-device-install-receipt.sh path/to/app.apk
```

For an Android 10-specific gate:

```text
RFA_REQUIRE_API29=1 bash ci/android-device-install-receipt.sh path/to/app.apk
```

The script:

1. requires exactly one ready device unless `ANDROID_SERIAL` is explicitly set;
2. verifies API level and an ARM ABI;
3. hashes the APK before installation;
4. executes `adb install -r --no-streaming`;
5. requires Android Package Manager to return the installed package path;
6. reads bounded package version/ABI metadata;
7. starts `io.rafaelia.audiostudio/.MainActivity` with `am start -W`;
8. writes `android_device_install_receipt_v1.txt`.

The raw device serial is not written to the receipt; only its SHA-256 is recorded. The script does not collect SSID, BSSID, MAC address or account identifiers, and it does not uninstall the existing application.

A successful run may promote only:

```text
INSTALLED_PHYSICAL=PASS
LAUNCH_SMOKE=PASS
```

It does **not** promote microphone, Ogg/Opus, WhatsApp, CFR, loudness or acoustic claims; those stay `NOT_RUN` until separately executed.

## In-app method

Tap **Gerar provas + teste μ∆**.

The bundle records:

```text
package
version_name
version_code
first_install_epoch_ms
last_update_epoch_ms
installed_apk_sha256
source_sha
ci_run_id
ci_run_number
```

When the APK was built by canonical CI, `ci_provenance_v1.txt` is embedded after native gates pass.

## Verification chain

```text
CI source SHA
-> built APK
-> static installability gate
-> Android Package Manager install
-> launched package
-> installed APK bytes
-> SHA-256 exported by installed process
```

The chain demonstrates byte identity and installation coordinates. Physical behavior still requires physical tests.

## TOKEN_VAZIO

Local/non-CI builds may legitimately report missing CI coordinates. That state is informative and must not be replaced by a guessed commit.
