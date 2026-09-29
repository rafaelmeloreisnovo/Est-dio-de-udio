# Security & Trust Boundaries

## Protected by design

- no backup of app private data;
- bounded custom container fields;
- raw/source preservation;
- narrow JNI bridge;
- freestanding core dependency gates;
- public C ABI manifest;
- installed APK SHA-256 in evidence bundle.

## Not guaranteed

- cryptographic authenticity of every external input;
- anti-tamper against a compromised OS;
- secure hardware attestation;
- privacy of files after the user shares them;
- scientific validity from hashes alone.

## Sensitive data

Audio recordings and hardware/sensor evidence can contain personal/device context. Share evidence bundles deliberately and minimize unrelated recordings.

Security findings should be documented with source, reproduction conditions and evidence rather than speculative severity.
