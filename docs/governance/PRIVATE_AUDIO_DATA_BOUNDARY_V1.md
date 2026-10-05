# Private Audio/Data Boundary V1

Status: CANONICAL_DRAFT
Scope: Est-dio-de-udio

## Purpose

This repository is a private producer for audio/app implementation and related contracts. Audio samples, recordings, personal material, credentials, build secrets, device details, and unpublished artifacts must be handled under private-by-default governance.

## Boundary

- Code and build contracts may be documented and tested.
- Private audio/data payloads are not public release material by default.
- Personal, sacred, spiritual, family, or identity artifacts must not be copied to public repositories by inference.
- A generated APK, report, or receipt is an artifact, not proof of every claim.

## Privacy Classes

| Class | Handling |
| --- | --- |
| PRIVATE_AUDIO | keep private; export only with explicit target authorization |
| PRIVATE_DATA | keep private; use hashes/opaque references when possible |
| PRIVATE_CODE | keep private unless release gate passes |
| PUBLIC_CODE_CANDIDATE | review license, secrets, and provenance before release |
| SECRET | block; rotate if exposed |
| SACRED_PERSONAL | private by default; no public copying by inference |
| TOKEN_VAZIO | block promotion |

## Release Gate

```yaml
release_gate:
  source_path: ""
  source_ref: ""
  artifact_type: code|audio|apk|doc|receipt|data|TOKEN_VAZIO
  privacy_class: TOKEN_VAZIO
  license_status: TOKEN_VAZIO
  contains_secret: TOKEN_VAZIO
  contains_personal_data: TOKEN_VAZIO
  contains_sacred_personal_content: TOKEN_VAZIO
  evidence_rule: TOKEN_VAZIO
  target_repo: ""
  target_path: ""
  rollback_ref: TOKEN_VAZIO
  claim_allowed: false
```

If any required field is TOKEN_VAZIO, do not release.

## Evidence Rule

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

A build artifact may be IMPLEMENTED_UNTESTED. It becomes PASS only after the required test evidence is observed and bound to the source/ref.
