#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
fail() { printf 'CI_LANE_CONTRACT_FAIL=%s\n' "$1" >&2; exit 1; }
wf='.github/workflows/START.yml'
quick='ci/rafaelia-fast-gate.sh'
test -s "$wf" && test -s "$quick" || fail 'CONFIG_MISSING'

# Every continuously changing draft emits only source+host evidence.
grep -Fq 'types: [opened, synchronize, reopened, ready_for_review, converted_to_draft]' "$wf" || fail 'PR_READY_TRANSITION_MISSING'
grep -Fq 'if test "$PR_DRAFT" = true; then' "$wf" || fail 'DRAFT_NOT_CLASSIFIED'
grep -Fq 'validation_lane="fast"' "$wf" || fail 'FAST_LANE_NOT_CONFIGURED'
grep -Fq 'validation_lane="full"' "$wf" || fail 'FULL_LANE_NOT_CONFIGURED'
grep -Fq 'if: ${{ needs.plan.outputs.validation_lane == '\''fast'\'' && github.event_name == '\''pull_request'\'' }}' "$wf" || fail 'FAST_NOT_BOUND_TO_PR'
grep -Fq 'if: ${{ needs.plan.outputs.validation_lane == '\''full'\'' && needs.plan.outputs.delivery != '\''provider-bootstrap'\'' }}' "$wf" || fail 'FULL_BUILD_NOT_BOUNDED'
grep -Fq 'name: gate / build' "$wf" || fail 'REQUIRED_FULL_BUILD_CONTEXT_CHANGED'
grep -Fq 'name: gate / quick (source + host; NOT APK)' "$wf" || fail 'FAST_SCOPE_MISLABELED'
grep -Fq 'cancel-in-progress: ${{ github.event_name == '\''pull_request'\'' || github.event_name == '\''push'\'' }}' "$wf" || fail 'OBSOLETE_QUEUE_NOT_CANCELLED'
grep -Fq "github.event.pull_request.number || github.ref" "$wf" || fail 'CONCURRENCY_NOT_PER_PR'
grep -Fq 'APK_BUILT=NOT_RUN' "$wf" || fail 'FAST_CLAIM_BOUNDARY_MISSING'
grep -Fq 'RELEASE_ELIGIBLE=false' "$wf" || fail 'FAST_RELEASE_BYPASS'
grep -Fq 'github.actor == '\''rafaelmeloreisnovo'\''' "$wf" || fail 'PROVIDER_OWNER_GATE_REGRESSION'
grep -Fq "needs.plan.outputs.delivery == 'provider-bootstrap'" "$wf" || fail 'MANUAL_ADMIN_REGRESSION'

# No SDK/Gradle/distribution run is executed by the fast entry point.
if grep -nE '^([^#]*)(sdkmanager|gradle[[:space:]]+:app|rafaelia-pipeline.sh[[:space:]]+bootstrap|publish-live|publish-signed|distribution-release.sh)' "$quick"; then
  fail 'FAST_REINTRODUCED_HEAVY_TOOLCHAIN_OR_DELIVERY'
fi
grep -Fq 'FAST_APK_AND_ABI_CROSS_BUILD=NOT_RUN' "$quick" || fail 'FAST_CROSSABI_BOUNDARY_MISSING'

# Signed delivery must load the exact source before running the repository-owned
# enforcement script, and must still enforce before any signing/bootstrap.
awk '
  /^  deliver_signed:/ { signed=1; next }
  signed && /^  [a-z_]+:/ { signed=0 }
  signed && /- name: Exact checkout for signed delivery/ { checkout=NR }
  signed && /- name: Provider enforcement gate/ { enforce=NR }
  signed && /- name: Signing capability bootstrap/ { bootstrap=NR }
  END { exit !(checkout > 0 && enforce > checkout && bootstrap > enforce) }
' "$wf" || fail 'SIGNED_ENFORCEMENT_ORDER_REGRESSION'
test -s ci/provider-enforcement-gate.sh || fail 'SIGNED_ENFORCEMENT_SOURCE_MISSING'

printf 'CI_LANE_ROUTING=PASS_SOURCE_CONTRACT\n'
printf 'FAST_IS_NOT_FULL_BUILD=PASS_CLAIM_CONTRACT\n'
printf 'P0_PROVIDER_BOOTSTRAP=UNCHANGED_OWNER_ONLY\n'
