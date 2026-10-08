# P0 CI queue containment — EstudioAudio

Author / copyright: Rafael Melo Reis. License and existing attribution retained.
Baseline before this source change: main@21314c3f04b93ad653223ccbea86d93b60075148.

## Observed problem vs real runtime

GitHub Actions pull request #52, run 37722854000, was created 2026-10-08T03:28:06Z and completed 03:59:21Z. Its **actual build job logs** start 03:57:00 and finish 03:59:18: about 2m18s of runner execution. Most of its ~31m wall-clock latency was GitHub runner queue/scheduling — not 31 minutes of compilation. It passed `gate / build`; earlier runs were canceled as new commits arrived.

Main push 37723299751 was in queued / provider-verification state at last observation. Provider main `protected=false` remains a separate P0; queue work cannot fake enforcement.

## Hotfix — 2 lanes with distinct claim authority

| Trigger | Run | Allowed claim |
|---|---|---|
| PR marked Draft; opened / synchronize / converted_to_draft / reopened | `gate / quick (source + host; NOT APK)` | source, JS-less shell contracts, JVM smoke, freestanding host C vectors |
| PR marked Ready for review; commits to ready PR | existing `gate / build` | full Java+NDK ARM32/ARM64, fuzz, reproducible APK, installability-static gates |
| `push main` | existing `gate / build` + `provider_observe` | binary receipt and explicit P0 provider readback |
| `workflow_dispatch validate/live-debug/signed/store` | existing full `gate / build` + appropriate delivery | full before any controlled delivery |
| `workflow_dispatch provider-bootstrap` | manual owner-only bootstrap, full gate skipped deliberately | authorized GitHub policy mutation only; no APK claim |

### Key invariants

- No fake green: `gate / quick` **is not** `gate / build`. Full gate retains the **same required status-check name**. A Draft PR has no full PASS and must not be merged as if it did.
- GitHub Events explicitly include `ready_for_review`; this triggers a new full run for the unchanged source SHA after an author finishes drafting. Once ready, every new commit again runs the full gate.
- New commits to the **same PR** cancel older PR jobs. New pushes to **main** cancel older obsolete push runs. **Manual deliveries are never cancelled automatically.**
- Workflow/job timeouts: plan 5m, quick 12m, full 45m of **runner time**. Timeouts cannot guarantee or measure GitHub queue wait time.
- `ci/rafaelia-fast-gate.sh` uses only installed runner host tools (javac, clang, shell, git). It avoids SDK manager, Gradle, APK packaging, emulator, signing, publishing, and remote actions.
- `ci/rafaelia-toolchain.sh` skips `sdkmanager` when exact pinned versions of Android SDK 36, build-tools 36.0.0, NDK 27.2.12479018 and CMake 3.22.1 are present. Missing components are still provisioned under explicit timeout then revalidated; cannot claim missing toolchains are present.
- `ci/ci-lane-contract-gate.sh` is called from both lanes and checks source-bound SHA, scope-labeled quick result, full gate identity, stable per-PR concurrency, admin auth and no fast release eligibility.

## Human operational route (do not force 2h validation for every code edit)

1. Open new PR as **Draft**, group related microcommits, and inspect **gate / quick**. New commits discard obsolete checks.
2. When development is stable, change Draft → **Ready for review**. That causes the full `gate / build` at the exact PR head.
3. Only after full success and independent rights/review gates, merge. Main may run its own full proof and provider observation.
4. If a full job fails, use GitHub **Re-run failed jobs** on the **same immutable SHA**; do not generate empty/new commits merely to restart an identical job.
5. If protection is unconfigured, keep P0 **FAIL** and request owner-only `provider-bootstrap` after configuring environment and secret. Do not remove `provider_observe`.
6. Use `workflow_dispatch mode=live-debug` when a physical device needs an exact APK — this remains an explicit full validation path.

## Explicit limitations / risks

- Draft fast lane does **not** test Android resource compilation, real ARM ELF ABI, APK installability, reproducibility, or physical device execution. They remain `NOT_RUN`.
- A contributor who can merge into **unprotected main** may bypass source rules despite these gates; P0 GitHub branch protection must be configured out of band.
- A GitHub runner availability incident, plan/account budget limit or billing quota can still cause large **queue waiting times**. Workflow code cannot compel GitHub to provide a runner. Avoid emitting 20 runs from 20 one-line commits.
- A manual production release should never be cancelled by auto-debouncing. PR #52/#53 have historical receipts separate from this change.
- Revert this PR to restore old every-PR full behavior; revert does not alter historical runs or GitHub provider permissions.

`R3=⟨F_ok: quick source lane and obsolete-run cancellation, F_gap: exact-head CI validation, GitHub runner queue, branch protection P0, physical APK, F_next: confirm quick success on draft → ready full success → exact-main and admin readback⟩`.
