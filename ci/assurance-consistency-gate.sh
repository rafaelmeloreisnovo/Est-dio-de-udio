#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

writer='app/src/main/java/io/rafaelia/audiostudio/AssuranceZiprafWriter.java'

afail() {
  printf '[ASSURANCE_CONSISTENCY_GATE][FAIL] %s\n' "$*" >&2
  exit 1
}

test -s "$writer" || afail "MISSING=$writer"

grep -Fq 'final int embeddedArtifactCount = 1; // raw evidence bytes copied into ZIPRAF' "$writer" || \
  afail 'EMBEDDED_ARTIFACT_BASE_SCOPE_MISSING'
grep -Fq 'int referencedArtifactCount = 1; // installed APK hash; bytes remain outside ZIPRAF' "$writer" || \
  afail 'REFERENCED_ARTIFACT_BASE_SCOPE_MISSING'
grep -Fq 'final int materializedCount = embeddedArtifactCount + referencedArtifactCount;' "$writer" || \
  afail 'MATERIALIZED_AGGREGATE_RELATION_MISSING'
grep -Fq '\"embedded_artifact_count\":' "$writer" || \
  afail 'EMBEDDED_ARTIFACT_COUNT_FIELD_MISSING'
grep -Fq '\"referenced_artifact_count\":' "$writer" || \
  afail 'REFERENCED_ARTIFACT_COUNT_FIELD_MISSING'
grep -Fq '\"installed_apk_custody\":\"REFERENCED_NOT_EMBEDDED\"' "$writer" || \
  afail 'INSTALLED_APK_CUSTODY_SCOPE_MISSING'
grep -Fq '\"custody\":\"EMBEDDED\"' "$writer" || \
  afail 'RAW_EVIDENCE_EMBEDDED_SCOPE_MISSING'
grep -Fq 'custody_boundary=EMBEDDED_BYTES_SEPARATE_FROM_HASH_REFERENCES' "$writer" || \
  afail 'CUSTODY_BOUNDARY_RECEIPT_MISSING'
grep -Fq '\"installed_apk_sha256\":\"' "$writer" || \
  afail 'MATERIALIZED_APK_EXPLICIT_FIELD_MISSING'
grep -Fq 'authorialSigningResolved' "$writer" || \
  afail 'AUTHORIAL_SIGNING_PREDICATE_MISSING'
grep -Fq 'hasEvidenceLine(rawEvidenceBytes, "signing_cert_matches_expected", "PASS")' "$writer" || \
  afail 'INSTALLED_CERT_MATCH_BINDING_MISSING'
grep -Fq 'gap("authorial_signing", "TOKEN_VAZIO_AUTHORIAL_SIGNATURE"' "$writer" || \
  afail 'AUTHORIAL_SIGNING_GAP_MISSING'
grep -Fq 'claim("authorial_signing_identity"' "$writer" || \
  afail 'AUTHORIAL_SIGNING_CLAIM_BOUNDARY_MISSING'
grep -Fq 'authorial_signing_binding=' "$writer" || \
  afail 'AUTHORIAL_SIGNING_RECEIPT_MISSING'

base_gap_line="$(grep -F 'int gapCount = 3; // physical SPL reference + independent reproduction + external audit' "$writer" | wc -l | tr -d ' ')"
test "$base_gap_line" = 1 || afail "BASE_GAP_DECLARATION_COUNT=$base_gap_line"

grep -Fq 'if (!authorialSigningResolved) ++gapCount;' "$writer" || \
  afail 'AUTHORIAL_SIGNING_NOT_COUNTED_AS_GAP'

printf 'MATERIALIZED_BASE=EMBEDDED_RAW_EVIDENCE+REFERENCED_INSTALLED_APK\n'
printf 'MATERIALIZED_AGGREGATE=EMBEDDED+REFERENCED\n'
printf 'ZIPRAF_EMBEDDED_REFERENCE_SPLIT=PASS_EXECUTED_SCOPE\n'
printf 'MATERIALIZED_APK_FIELD=EXPLICIT\n'
printf 'AUTHORIAL_SIGNING_GAP=EXPLICIT_FAIL_CLOSED\n'
printf 'AUTHORIAL_CERT_MATCH_SOURCE=RAW_PHYSICAL_EVIDENCE\n'
printf 'ASSURANCE_CONSISTENCY_GATE=PASS_EXECUTED_SCOPE\n'
printf 'CLAIM_ALLOWED=false\n'
