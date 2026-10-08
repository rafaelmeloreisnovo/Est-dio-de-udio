# EstudioAudio — P0 provider enforcement + audio session + freestanding hardening

**Autor:** Rafael Melo Reis  
**Canonical baseline:** `main@15586b5b9d59f3cf905ccfa18d99dd7baae512e0` (2026-10-07)
**Classification:** SOURCE PATCHED / exact-HEAD CI TOKEN_VAZIO until verified / physical Android TOKEN_VAZIO.

## A. Diagnosis (do not confuse provider with source)

- Latest observed main run `37618036052`: `gate / build=SUCCESS`, `provider / bootstrap=FAIL` after HTTP 403. The provider error said the selected branch-protection feature required account/repository capability at that execution time.
- The repository is now publicly readable, but the current branch readback still reported `main.protected=false` and no required checks. A changed visibility or completed build does not establish enforcement.
- Original `START.yml` attempted administrator mutation on **every green push**, sourcing the administrative token from `PAT_ENVIRONMENTS || PAT_ENV || PAT_ACTIONS`. This crosses least-privilege boundaries and couples software gate state to provider mutation failures.
- Original recording worker could encounter an audio read/write failure and still expose partial PCM; `stop()` could return after 1500ms without a finished writer; concurrent masters shared a fixed `rafaelia_mastered_48k.pcm` path and JNI global DSP/meter state.

## B. P0 source changes

1. **Routine main push** now runs a **read-only** `provider_observe` job. If build has passed but `main.protected=false` or its exact head has moved, it fails visibly and never borrows a privileged PAT. A green build alone is not a green complete push pipeline.
2. **Administrative mutation** only from `workflow_dispatch` `mode=provider-bootstrap` with `github.actor=rafaelmeloreisnovo`, `ref=refs/heads/main`, matching `GITHUB_SHA=RFA_SOURCE_SHA=live main SHA`. There are no fallbacks to PAT_ACTIONS/PAT_ENV/other capabilities.
3. Required secret: `PAT_ENVIRONMENTS`, provisioned by the repository owner with only the admin capabilities needed for this exact repository. The configured token value and scopes are not inspected by this repository audit. Missing/denied credentials remain `TOKEN_VAZIO / FAIL`.
4. Bootstrap will initialize protection only when main is **currently unprotected**. If protected already, it **never overwrites stronger existing reviews/restrictions**; instead it reads the policy and requires the exact `gate / build` context, strict mode, admin enforcement, force-push/delete blocked and conversation resolution. It does not claim success from `.protected=true` alone.
5. Release-like delivery routes (`live-debug`, `signed-release`, Play) require the owner actor. Signed/Play further require protected main ref and exact SHA via the existing `ci/provider-enforcement-gate.sh`. No change publishes a new APK automatically.

### Operator route after source CI has passed

- **STEP 1**: Review + merge this PR only after the **exact PR SHA** software tests are green and rights reviewed.
- **STEP 2**: Configure/confirm the repository's **fine-grained PAT_ENVIRONMENTS** with repository administration permission, environment management as needed; do not copy a CI PAT to an APK or logs.
- **STEP 3**: GitHub Actions > **Rafaelia Audio START** > Run workflow from `main` with **mode: provider-bootstrap** and `source_sha` empty (uses exact current main). Only the authorized owner should initiate this operation.
- **STEP 4**: Capture the provider bootstrap run ID, HTTP outcome and branch-protection readback, including required check and review settings. If denied by plan/permissions, keep P0 open; **do not disable the read-only guard** or use a public/private visibility change as a silent workaround.
- **STEP 5**: Trigger validation on the protected branch and confirm the next post-merge `provider_observe` succeeded at the exact SHA; verify denied bypass/force-push/deletion on a **safe test branch/fixture** rather than risking project assets.
- **STEP 6**: If no provider-admin capability is available, retain a red P0 `provider_observe` while investigating an authorized GitHub Rulesets/branch-protection path. Neither Source CI nor this patch can grant account permissions.

## C. Runtime reliability changes

- New project-owned `AudioProcessingGate.java` wraps `AtomicBoolean` with explicit `tryEnter/busy/leave`; JVM host smoke stresses 12 concurrent workers without Android or third-party packages.
- MainActivity disallows simultaneous mastering, relative acoustic calibration, raw capture or in-app ZIPRAF acquisition at a shared JNI DSP/meter boundary. Stale/imported requests are refused.
- Every capture, imported audio and mastering session receives a **unique local PCM path**; no shared `rafaelia_mastered_48k.pcm` overwrite. Failed master files are removed; successful files persist for playback and evidence.
- AudioRecord negative read or I/O exception sets a failure state; `stop()` returns true only after the worker drains and closes, otherwise no mastering. Timed-out workers defer Android native buffer release until completion. The raw file remains for diagnostics.
- No new third-party app runtime dependencies, no AndroidX/Kotlin/Node, no change to the Android JNI/hosted I/O boundary. Do not relabel the final platform-linked ELF freestanding.

## D. Expanded tests

- `ci/audio-session-gate.sh`: compile pure-Java gate and run `native/tests/AudioProcessingGateSmoke.java`; inspect fail-closed Android call-site contracts.
- `ci/test-provider-preflight.sh`: offline negative preflight tests for event, actor, ref, SHA and missing dedicated PAT; also rejects workflow PAT capability fallback.
- `ci/rafaelia-pipeline.sh quality`: both mandatory gates before APK build.
- `ci/freestanding-runtime-gate.sh`: expands the ARMv7 + AArch64 zero-undefined, `-Wshadow -Werror` and no-writable-state checks from 13 to **15** authorial C units (adds `rfa_audio_format_core`, `rfa_verbo_core`).

## E. Closure matrix

| Gate | State at source commit | Required terminal evidence |
|---|---|---|
| Authorship and license | project-authored delta; original license retained | owner review and attribution |
| Static source structure | implemented | changed-file readback, tests |
| Exact-head CI | TOKEN_VAZIO before run | PR run ID + jobs + PASS/FAIL |
| Real GitHub branch enforcement | **FAIL — main observed unprotected** | admin API readback and bypass rejection |
| Android compile/ABI | prior main software PASS | exact new PR SHA run |
| Physical ARM32/ARM64 audio | NOT_RUN | exact APK/install/device receipts |
| User ZIPRAF and signed APK | TOKEN_VAZIO | exact source→artifact→device→evidence chain |
| Acoustic standards | NOT_AUDITED | independently validated vectors/reference |

## F. Rollback

- Revert this feature branch/PR for source rollback; no destructive repository operation is performed by these commits.
- P0 provider mutations, if later executed manually, are separate GitHub configuration state and require explicit reviewed rollback; **never** delete protection solely because the source PR was reverted.
- Audio legacy PCM filenames are no longer shared. Existing raw/master/ZRF files are untouched by this patch, and successful new results are retained for evidence.

`R3 = ⟨F_ok: source-level isolation + race prevention + stronger C gates, F_gap: exact-head CI + GitHub admin readback + physical exact-byte device receipt, F_next: run CI; privileged provider-bootstrap by owner; real device end-to-end⟩`.
