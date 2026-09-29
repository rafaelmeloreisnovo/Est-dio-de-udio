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

The canonical workflow now permits **no external JavaScript Action**.

```text
EXTERNAL_JS_ACTIONS = NONE
```

Successful main builds publish the debug APK through the runner-provided GitHub CLI as a traceable prerelease. `gh` remains an external GitHub platform tool and is not reclassified as project-authored.

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

Permitted external JavaScript Action set:

```text
{}
```

Any external Action fails the gate.

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
