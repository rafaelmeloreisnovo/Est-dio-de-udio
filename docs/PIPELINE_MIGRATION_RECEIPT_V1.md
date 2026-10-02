# PIPELINE_MIGRATION_RECEIPT_V1

## Source boundary

Pre-refactor main:

```text
299a877587c00aedcd6fbb327f94271d26d230b7
```

Active workflows observed before migration:

```text
.github/workflows/android.yml
  blob=998a7ee553935038ee3dc0d29a68ae50a1b43df4
.github/workflows/audio-gate.yml
  blob=484f940c899e2b14eabf5d346f226ee153b8351b
.github/workflows/pr-android.yml
  blob=4cffe9180d7c4b919240be720a498dda145f8729
.github/workflows/signed-release.yml
  blob=3cbde235b1ff0921766567d50f3a03c7d43cb6b1
```

## Target topology

```text
.github/workflows/START.yml
ci/rafaelia-pipeline.sh
docs/PIPELINE_CAPABILITY_QUALITY_V1.md
```

Expected active root invariant:

```text
ACTIVE_WORKFLOW_COUNT=1
CANONICAL_WORKFLOW=.github/workflows/START.yml
```

## Behavior mapping

```text
old pr-android.yml
  -> START.yml event=pull_request route=validate

old android.yml
  -> START.yml event=push main route=live-debug

old audio-gate.yml
  -> ci/rafaelia-pipeline.sh phases + START.yml gate job

old signed-release.yml
  -> START.yml workflow_dispatch mode=signed-release
```

## Evidence boundary

At document creation time the refactor is source material only until the exact-head pull-request workflow completes.

```text
SOURCE=IMPLEMENTED
EXECUTION=PENDING_EXACT_HEAD_CI
EVIDENCE=PENDING_EXACT_HEAD_CI
CLAIM_ALLOWED=false
EXTERNAL_STANDARD_AUDIT=NOT_AUDITED
```

## Rollback

No history rewrite or force push is part of this migration. Retired workflow content remains in Git history at the pre-refactor main SHA and its ancestor blobs. A normal revert restores the previous active topology.
