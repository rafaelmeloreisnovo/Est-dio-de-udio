# Codebook & Provenance V1

## Principle

```text
symbol != content
symbol + codebook + index + provenance -> reconstruction route
```

## ZRF registered chunks

- `CODE` — versioned codebook/schema identifiers and semantic mappings.
- `PROV` — origin/version/commit/receipt coordinates.
- `RAC1` — experimental RAC1 coded PCM payload.

Registration of a chunk type does not mean every ZRF file contains that chunk.

## Minimum codebook identity

A future CODE payload should declare at least:

```text
codebook_id
schema_version
content_type
unit/scale
dimensions
field identifiers
transform identifier
```

## Minimum provenance identity

A future PROV payload should declare at least:

```text
format_id
author/project id
schema_version
source digest
encoder/transform version
commit/source SHA
receipt id
timestamp when applicable
external signature reference when available
```

## Boundary

Internal hash = integrity coordinate.

```text
integrity != authenticity
hash != authorship
```

Cryptographic/authorship claims need their own external trust anchor.
