# Installation Validation

## Goal

Answer: **which exact application bytes are installed on this phone?**

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
-> installed APK bytes
-> SHA-256 exported by installed process
```

The chain demonstrates byte identity coordinates. Physical behavior still requires physical tests.

## TOKEN_VAZIO

Local/non-CI builds may legitimately report missing CI coordinates. That state is informative and must not be replaced by a guessed commit.
