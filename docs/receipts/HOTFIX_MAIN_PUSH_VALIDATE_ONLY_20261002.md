# HOTFIX — main push validate-only — 2026-10-02

## Authority

- repository: `rafaelmeloreisnovo/Est-dio-de-udio`
- base main: `e03f1ec7fa2cce0fe0eb3d0c884c58b56493e277`
- hotfix branch: `hotfix/main-push-no-auto-delivery-20261002`
- source workflow: `.github/workflows/START.yml`

## Observed prestate

Provider readback on the base showed `main` unprotected. The canonical START route selected `delivery=live-debug` for every `push` event on `main`, so a direct main write could automatically reach the prerelease delivery job after the quality gate.

## Delta

`push(main)` now resolves to `delivery=validate`.

Only `workflow_dispatch` may explicitly request `live-debug` or `signed-release`.

No build, DSP, ABI, signing, APK, evidence, or scientific semantics are changed by this hotfix.

## Invariants

- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`
- `IMPLEMENTED_UNTESTED != PASS`
- `CI_PASS != PHYSICAL_DEVICE_PASS`
- `REFERENCE_TO_STANDARD != AUDITED_CONFORMITY`
- `external_standard_audit=NOT_AUDITED`
- `claim_allowed=false`

## Gate

The exact hotfix head must complete the pull-request `Rafaelia Audio START` successfully before merge. On PR events, both delivery jobs must remain skipped because the route is `validate`.

## Remaining gap

Server-side branch protection for `main` is still a separate provider control and remains unresolved by this source-only change.

## Rollback

Revert the hotfix commit or close the PR. No force-push, history rewrite, secret rotation, or destructive migration is required.

## R3

`F_ok=automatic delivery removed from ordinary main push path`

`F_gap=provider-side main protection remains absent`

`F_next=exact-head CI; then provider enforcement as a separate bounded control`
