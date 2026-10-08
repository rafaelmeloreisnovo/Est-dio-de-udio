# RAFAELIA Audio — Java low-level / ZIPRAF byte serializer

**Copyright:** Rafael Melo Reis (2026)  
**License:** `LicenseRef-RAFCODE-Research-Commercial-0.1`  
**Base source:** `main@61dd30aab2c3673cb1bc4c803546e71157405d4b`  
**Evidence boundary:** implementation does not equal host execution; host execution does not equal real-device execution.

## Request interpretation and irreducible runtime

Low-level Java can avoid third-party libraries, reflection, native SDK calls *inside a particular algorithm*, providers and heavyweight object abstractions. **Java bytecode itself cannot become an OS-free ELF or bare-metal freestanding program**: a working Android APK requires ART, framework services, Binder, audio I/O, signing/package management and the app's Java↔JNI boundary. It is unsafe to delete those and claim the same app still works.

This delta removes a genuine high-value hosted-library dependency from the evidence-critical ZIPRAF path, rather than pretending the complete APK is freestanding.

## Prior/next boundary

| Item | Baseline on main | New branch | Scope |
|---|---|---|---|
| ZIP archive | `java.util.zip.ZipOutputStream` | project `RfaStoredZip` | ZIP32 STORED only |
| ZIP entry metadata | `java.util.zip.ZipEntry` | fixed-layout LE bytes | central directory + local header |
| CRC32 | `java.util.zip.CRC32` | authorial reflected polynomial `0xedb88320` | KAT + standard parser comparison |
| Evidence bytes | `ByteArrayOutputStream` | `RfaBoundedBytes` | cap 8 MiB per entry |
| Maximum archive | no dedicated project limit | 64 MiB total / 32 entries | fail-closed |
| ZIP path | library default | ASCII [A-Za-z0-9_./-] without traversal | fixed ZIPRAF names |
| Sorted entry collection | `TreeMap<String, byte[]>` + `Map.Entry` | `RfaOrderedEntries` fixed 32-slot arrays with insert-sort | no `TreeMap` / `Map` in ZIP writer |
| Storage | Android `MediaStore` | **retained** | indispensable platform I/O |
| SHA256 | authorial `LowSha256` | **retained** | no provider dependency |
| DSP | C freestanding + shared JNI edge | **unchanged** | no DSP implementation duplication |
| Other Android codecs | platform `MediaCodec` / `MediaMuxer` | **retained** | no unsupported Opus claim |

The new ZIP archive uses fixed DOS timestamp `1980-01-01 00:00:00`; this differs from the former Java `setTime(0L)` encoding/extra fields. **The ZIPRAF container bytes will change**, even though entry names, payload bytes and `99_SHA256SUMS.txt` semantics remain intact. Do not claim old ZIP and new ZIP have the same SHA256.

## Code route

```text
★ VALIDAR + ZIPRAF [user]
→ Android sensor/raw evidence collection [hosted]
→ RfaBoundedBytes [project-owned primitive-copy, bounded]
→ LowSha256 [project-owned]
→ RfaOrderedEntries (fixed 32-slot sorted arrays)
→ RfaStoredZip.add(name, bytes)
     - validate ASCII name, duplicate, counts, size
     - bitwise CRC32 per byte
     - encode ZIP32 little-endian local header
     - write payload, record offset/checksum/size
→ RfaStoredZip.finish()
     - write central directory + EOCD
→ Android MediaStore output [hosted]
→ installed artifact/evidence/claim verification [not implied by source]
```

## Tests and exact evidence

CI quality gate `ci/zipraf-lowlevel-gate.sh` compiles with `javac` and executes:
- deterministic archive byte identity for repeated identical inputs;
- `CRC32("")=0` and `CRC32("123456789")=0xCBF43926` known-answer tests;
- `java.util.zip.ZipFile` **test-only** reader, verifying names, stored method, size, CRC and full extracted entry bytes;
- test-only JDK CRC32 oracle versus authorial bitwise CRC32 on deterministic pseudo-random 32 KiB payload; sorted insertion and duplicate rejection in the fixed array table;
- negative cases: parent traversal, leading slash, backslash, non-ASCII, double slash, duplicate entry, >8 MiB data, >32 entries, write after finish, bounded buffer overflow/invalid slice;
- source-level gate rejecting `java.util.zip` and `ByteArrayOutputStream` imports from shipped Java, and `java.util.Map/TreeMap` from ZIPRAF writer.

This is an initial deterministic host compatibility oracle. An independent ZIP parser/unzip implementation, streaming stress on a physical Android device and ZIPRAF readback receipts should be performed separately.

## P0 provider status remains separate

At baseline readback on 2026-10-08:
```text
main             = 61dd30aab2c3673cb1bc4c803546e71157405d4b
main.protected   = false
main required checks = []
last P0 PR #52   = merged; provider enforcement is NOT proven
```

No privileged GitHub policy mutation, package signing, APK installation or external publication is performed by this low-level delta. Do not weaken `provider_observe` to obtain green CI.

## How to reproduce

1. Read `AGENTS.md`, `docs/START_HERE.md` and this file.
2. Checkout the exact candidate source SHA.
3. Run `bash ci/zipraf-lowlevel-gate.sh` with JDK 17+.
4. Run existing `ci/rafaelia-pipeline.sh quality` and the canonical `gate / build`; retain CI run ID, commit SHA and test statuses.
5. Build both `armeabi-v7a` and `arm64-v8a`; verify no changes to C core ABI/JNI symbols.
6. On physical Android, generate ZIPRAF using `★ VALIDAR + ZIPRAF`, export it and independently test central directory, per-entry CRC32, extracted bytes and `99_SHA256SUMS.txt` bindings.
7. Capture exact installed APK SHA256, signer identity, source SHA, CI ID and device receipt. Keep `claim_allowed=false` for unvalidated acoustic metrics.

## Risks / Rollback

- Internal ZIP serialization can introduce compatibility bugs: host `ZipFile` oracle and device roundtrip are the falsifiers.
- Data greater than 8 MiB now fails explicitly instead of exhausting memory; this is an intentional bounded scope reduction. Larger file support requires a separately tested streaming/paged ZIP writer.
- New ZIP bytes intentionally differ from JDK metadata; any external process incorrectly trusting the whole ZIP digest as fixed across producer implementations must be updated by a new receipt, never bypassing integrity verification.
- To rollback code, revert this change (no schema migration). Historic ZIPRAF files remain normal ZIP/STORED files; their old hashes remain valid historical evidence.
- Owner-only provider bootstrap and release governance from PR #52 are unchanged. P0 branch protection still demands provider readback.

`R3 = ⟨F_ok: source authorial ZIP32/CRC/byte buffer and deterministic oracle, F_gap: CI exact head and device proof, F_next: host → PR CI → source↔APK↔device receipt → provider P0 admin closure⟩`.
