#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"
fail() { printf 'AUDIO_SESSION_GATE_FAIL=%s\n' "$1" >&2; exit 1; }

main='app/src/main/java/io/rafaelia/audiostudio/MainActivity.java'
engine='app/src/main/java/io/rafaelia/audiostudio/AudioRecorderEngine.java'
policy='app/src/main/java/io/rafaelia/audiostudio/AudioProcessingGate.java'
test -s "$main" && test -s "$engine" && test -s "$policy" || fail 'SOURCE_MISSING'

# Fail closed on regressions of the actual Android call path, not a parallel fake.
grep -Fq 'private final AudioProcessingGate processingGate' "$main" || fail 'SESSION_GATE_NOT_WIRED'
grep -Fq '!processingGate.tryEnter()' "$main" || fail 'MASTER_NO_SINGLEFLIGHT'
grep -Fq 'processingGate.leave()' "$main" || fail 'MASTER_NO_RELEASE'
grep -Fq 'processingGate.busy()' "$main" || fail 'CONCURRENT_PROOF_CAL_REC_NOT_GATED'
grep -Fq 'File.createTempFile("rafaelia_mastered_"' "$main" || fail 'MASTER_SHARED_FILENAME'
grep -Fq 'File.createTempFile("rafaelia_raw_"' "$main" || fail 'CAPTURE_SHARED_FILENAME'
grep -Fq 'File.createTempFile("rafaelia_imported_"' "$main" || fail 'DECODE_SHARED_FILENAME'
grep -Fq 'boolean captureComplete = recorder.stop();' "$main" || fail 'CAPTURE_COMPLETION_NOT_OBSERVED'
grep -Fq 'if (!captureComplete)' "$main" || fail 'PARTIAL_PCM_PROMOTION_RISK'
grep -Fq 'if (captureThread.isAlive())' "$engine" || fail 'CAPTURE_DRAIN_NOT_GATED'
grep -Fq 'captureFailed = true;' "$engine" || fail 'CAPTURE_IO_FAILURE_NOT_RECORDED'

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
javac -encoding UTF-8 -d "$tmp" "$policy" native/tests/AudioProcessingGateSmoke.java
java -cp "$tmp" io.rafaelia.audiostudio.AudioProcessingGateSmoke

echo 'AUDIO_SESSION_ANDROID_WIRING=PASS_SOURCE_CONTRACT'
echo 'AUDIO_SESSION_PROVIDER_NATIVE_RECEIPT=NOT_RUN'
echo 'AUDIO_SESSION_GATE=PASS_EXECUTED_SCOPE'
