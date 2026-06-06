# Phase 28B — OpenCPN lookup import and dynamic S-57 keys

Phase 28B keeps the Phase 28A raw OpenCPN inventory layer but adds the pieces
needed to import all OpenCPN lookup rows without losing entries that are outside
this project's current curated enum catalogues.

## Added

- `S57ObjectClassKey` and `S57AttributeKey` in `s52-catalog`.
- Compatibility helpers from existing `S57ObjectClass` / `S57Attribute` enums.
- Key-based accessors in `S57Attributes` for code that already has dynamic keys.
- `EncFeature.objectClassKey`, defaulted from the existing enum object class.
- Raw OpenCPN lookup model and parser for `chartsymbols.xml`.
- OpenCPN attrib-code parser for common forms:
  - `CATACH8`
  - `COLOUR3,1`
  - `DRVAL1?`
  - `CONDTN`
  - lowercase/alias forms like `fnctnm5` and `cattml3`
- Lookup diagnostics for unresolved symbols, line styles, patterns, CSP names,
  unsupported attrib-code forms, and unknown catalogue names.

## Important boundary

This phase does **not** switch browser rendering to the OpenCPN pack. Runtime
portrayal still uses the existing generated/compat pack unless callers explicitly
use the new raw inventory APIs. Phase 28C will generate the commonMain OpenCPN
source pack.

## Expected corrected-baseline counts

```text
lookups = 3057
table names = Plain, Symbolized, Simplified, Paper, Lines
private/internal lookup names preserved, including $AREAS, $LINES, $CSYMB, $TEXTS
```
