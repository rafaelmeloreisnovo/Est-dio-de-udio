# Authorial CI Toolchain V1

## Invariant

```text
AUTHORIAL_ORCHESTRATION != AUTHORIAL_RUNTIME
PROJECT_SCRIPT != THIRD_PARTY_TOOLCHAIN
PINNED_BINARY != PROJECT_AUTHORED_BINARY
```

The project owns the orchestration, validation rules, pins, gates and receipts.

It does **not** claim authorship of Git, Bash, Java, Android SDK/NDK, CMake, Gradle, Clang, GitHub Runner or GitHub artifact infrastructure.

## Node.js boundary

The application itself has no Node.js runtime.

```text
NODE_APP_RUNTIME = 0
NODE_PACKAGE_DEPS = 0
NPM/YARN/PNPM = NOT_USED
```

The canonical CI path no longer uses JavaScript Actions for:

- checkout;
- Java setup;
- Android setup;
- Gradle setup.

Those jobs are replaced with project-owned shell orchestration.

The canonical workflow permits **one narrowly pinned third-party GitHub Action dependency** solely for storing a successful full build as a downloadable workflow artifact. It runs on GitHub's hosted Node 24 action execution environment. This does not add Node, JavaScript or third-party libraries to the Android APK, nor does it give the full validation job `contents: write`.

```text
EXTERNAL_JS_ACTIONS = PINNED_UPLOAD_ARTIFACT_V7_ONLY
ACTION = actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9
ACTION_VERSION = v7.0.2
RUNTIME_BOUNDARY = HOSTED_GITHUB_CI_NODE24_ONLY
APK_NODE_RUNTIME = 0
APK_OUTPUT = DEBUG_NONAUTHORIAL, directly downloadable from full GitHub Actions run
APK_RECEIPTS = binary-origin + per-entry SHA-256, separate retained artifact
RETENTION = 14 days (subject to provider policy)
```

The uploader is owned by GitHub's `actions` organization, not the project; its source/license and immutable pin require review as an external component. The purpose is to retain bytes that were formerly deleted with the runner. No project-authored drop-in equivalent is claimed; replacement would require a supported and audited artifact transport. The existing `gh` transport remains the separately authorized **manual** `workflow_dispatch mode=live-debug` prerelease route. An ordinary main push does not automatically publish a GitHub Release, and branch protection P0 remains independent.

## Checkout

The reusable workflow performs a detached, SHA-bound Git checkout:

```text
git init
-> origin = current public repository
-> fetch exact RFA_SOURCE_SHA
-> checkout detached FETCH_HEAD
-> verify HEAD == RFA_SOURCE_SHA
```

## Java

The toolchain script requires Java 17 and verifies the runner-provided installation before use.

Absence is fail-closed:

```text
JAVA17_TOKEN_VAZIO -> FAIL
```

## Android

The runner-provided Android SDK root is observed and then the exact project-required packages are requested:

```text
platform-tools
platforms;android-35
build-tools;35.0.0
ndk;27.2.12479018
cmake;3.22.1
```

No claim is made that those tools are authored by Rafaelia.

## Gradle

Gradle 8.11.1 is downloaded from the official distribution endpoint and accepted only when SHA-256 matches:

```text
f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6
```

A mismatch fails closed.

## Dependency gate

The workflow scans all workflow `uses:` entries.

Permitted external JavaScript Action set (immutable SHA, exactly two invocations in full gate):

```text
{actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9}
```

All other external Actions or a changed SHA fail the topology gate. Contract tests bind upload after reproducibility, installability and binary receipt. Rollback: revert the hotfix PR to restore no external Actions and no automatic artifact retention.

Local reusable workflows remain allowed.

## Future reduction

Possible later stages:

1. pin the runner image instead of the moving `ubuntu-latest` label;
2. record deeper SDK package/tool identities in post-build receipts;
3. reproduce the build outside GitHub Actions;
4. compare GitHub CLI release transport with an independently hosted mirror.

```text
NODEJS_AUTHORIAL_RUNTIME = NOT_IMPLEMENTED_NOT_REQUIRED
```
