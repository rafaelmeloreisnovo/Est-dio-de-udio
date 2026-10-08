# P0 APK download delivery hotfix — 2026-10-08

Copyright: Rafael Melo Reis. Existing repository licensing and third-party provenance remain unchanged.
Source baseline: `main@5a65af2d37149c4e35ef28b286c7551819e9044d`.
Scope: GitHub Actions APK retention and evidence transport. No Android source, DSP or signing semantics changed.

## Independent observed facts

- [Main push run 37723299751](https://github.com/rafaelmeloreisnovo/EstudioAudio/actions/runs/37723299751): `gate / build=SUCCESS`; Java/ARM compile, debug build, exact-byte gate, static installability and binary receipt completed successfully.
- GitHub Actions run artifact API returned `artifacts=[]`: there was no retained APK to download.
- `deliver_live=SKIPPED` and `deliver_signed=SKIPPED`, because ordinary push(main) resolves to `delivery=validate` intentionally.
- `provider_observe=FAIL`: `PROVIDER_SOURCE_SHA=61dd30aab2c3673cb1bc4c803546e71157405d4b`, `PROVIDER_LIVE_MAIN_SHA=21314c3f04b93ad653223ccbea86d93b60075148`, `PROVIDER_MAIN_PROTECTED=false`. This is governance/freshness, not a compiler failure.
- A historical successful build is **not** retroactively downloadable if its runner bytes were not retained. A new exact-head run is necessary.

## Change, bounded authority

Full `gate / build` now, after `reproducible-debug-gate`, `apk-installability-static`, and `binary-receipt`:

1. validates that debug APK bytes exist and their SHA-256 matches the existing receipt;
2. copies exactly those bytes to a source/run-named `.apk` and checks byte equality;
3. invokes GitHub's pinned `actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9` (v7.0.2, Node 24) with `archive: false`, creating a direct-download APK, `if-no-files-found: error`;
4. retains the binary-origin receipt and per-entry SHA-256 manifest as a distinct artifact;
5. limits retention request to 14 days, subject to GitHub/provider policy.

Fast/Draft lane remains source/host-only and must never claim an APK. Manual `workflow_dispatch mode=live-debug` remains the separate prerelease publication operation. `push(main)` **does not** create a public GitHub Release. Full CI job retains `contents: read`; write-scoped delivery jobs remain separate.

## External-dependency and rights boundary

- Component: upstream `actions/upload-artifact`, owner `github.com/actions/upload-artifact`; immutable full Git SHA above.
- Purpose: GitHub workflow artifact-service transport, not Android runtime, DSP, packaging, or APK source code.
- Runtime: GitHub-hosted Node 24 action only; `NODE_APP_RUNTIME=0`, `NPM/YARN/PNPM=NOT_USED` by the app.
- Authorial status: **external**. The project does not assert copyright in the upstream action.
- Reason: GitHub Actions artifact-service upload support is a platform edge; an independently validated low-level equivalent is not present.
- License/terms: follow the original upstream LICENSE and the GitHub Actions service terms; no external source is copied/vendorized by this change. Independent legal determination remains outside these source gates.
- Policy gate: exactly this immutable action is allowed, exactly twice, after the binary receipt; unapproved external `uses:` fails topology checking.
- Rollback: revert this PR to restore previous no-JavaScript-actions CI; the APK retention gap will reappear. Previous builds/releases are not rewritten.

## Post-change evidence requirements

```text
SOURCE_CHANGED = YES
WORKFLOW_UPLOAD_EXECUTED = NOT_RUN
APK_ARTIFACT_RETAINED = NOT_RUN
BINARY_RECEIPT_ARTIFACT_RETAINED = NOT_RUN
DEVICE_INSTALLED = NOT_RUN
PHYSICAL_AUDIO = NOT_RUN
PROVIDER_MAIN_PROTECTION = FAIL_LAST_OBSERVED (re-read required)
CI_PASS_EXACT_PATCH_HEAD = NOT_RUN
CLAIM_ALLOWED_END_TO_END = false
```

Promotion requires a new full GitHub Actions run, `gate / build=SUCCESS`, download artifact list non-empty, comparison of downloaded APK SHA-256 to `binary_origin_receipt_v1.txt`, and (separately) a physical installation receipt for any device claim.

## Human download route

1. Open [Actions / Rafaelia Audio START](https://github.com/rafaelmeloreisnovo/EstudioAudio/actions/workflows/START.yml).
2. Choose a **full** run after this fix merges, or explicitly start `Run workflow → mode=validate` on the intended ref. Do not use a Draft PR quick-only run for APK.
3. Under **Artifacts**, download the direct `RafaeliaAudio-<SHA>-run<RUN_ID>.apk` and the separate `rafaelia-binary-receipts-<RUN_ID>`.
4. Verify the APK `sha256sum` against `apk_sha256` in the binary-origin receipt. The file is debug-signed; it is **not** an authorially signed/store APK.

## R3

`F_ok=source-level delivery path with SHA-bound direct APK + custody artifact proposed`

`F_gap=exact patch head CI, GitHub artifact readback, provider branch protection, device install`

`F_next=PR review and full green CI → artifact listing/hash proof → merge only with rights/security review → main readback`
