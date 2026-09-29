# TOKEN_GAP_RECONCILIATION_V1

## Rule

```text
unknown value + known reason -> explicit state
unknown value + resolvable runtime query -> query it
unknown value + required physical execution -> NOT_RUN/PENDING_PHYSICAL_REFERENCE
undefined semantics/source -> TOKEN_VAZIO
```

## Promoted from TOKEN_VAZIO

| Field | New state/source |
|---|---|
| recorder input before start | `NOT_STARTED` / `INPUT_IDLE` |
| null runtime input | `INPUT_UNAVAILABLE` |
| Android audio property absent | `UNAVAILABLE_NOT_REPORTED` |
| Android service absent | `UNAVAILABLE_SERVICE` |
| absolute SPL without reference | `PENDING_PHYSICAL_REFERENCE` |
| response curve before measurement | `NOT_RUN` |
| NPU generic Android query | `UNAVAILABLE_STANDARD_ANDROID_QUERY` |
| NUMA generic Android query | `UNAVAILABLE_STANDARD_ANDROID_QUERY` |

## Runtime-filled capability gaps

The evidence bundle now enumerates:

- audio input devices;
- product names;
- advertised sample rates;
- channel counts and masks;
- encodings;
- OpenGL ES required version;
- Vulkan hardware feature presence;
- ABI and Android runtime;
- sensor inventory.

## Intentionally retained TOKEN_VAZIO

- undefined `NIU 128` semantics until a concrete hardware/interface source exists;
- absolute physical values with neither measurement nor reference;
- historical receipt fields that truthfully recorded a prior unknown state;
- non-CI source/CI coordinates when a local build supplies none.

Historical receipts are append-only and are not rewritten to make the past look complete.
