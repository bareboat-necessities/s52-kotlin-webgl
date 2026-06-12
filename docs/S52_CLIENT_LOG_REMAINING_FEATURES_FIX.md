# S-52 client-log remaining feature coverage fix

This incremental patch targets the remaining feature-coverage diagnostics from
`US5NYCDF` after the project-log coverage patch.

The new client log already reports:

- `unsupportedAttributes=0`
- `unsupportedObjectClasses=0`
- `missingSymbols=0`
- `missingColorTokens=0`

The remaining catalogue gaps were:

- `s52.unmodeled_attribute=50`, all from typed S-57 attributes not yet in the
  runtime enum: `CATLND`, `CATSLO`, and `NATQUA`.
- `s52.unmodeled_primitive=1`, from `ACHBRT` line geometry aliased through
  `ACHARE` while OpenCPN's raw lookup table only contained `ACHARE` area rows.
- `s52.unmodeled_object_class=1`, from the S-57 placeholder object key
  `OBJL_0` appearing as a line feature.

## Changes

- Added typed attributes `CATLND`, `CATSLO`, and `NATQUA` to `S57Attribute`.
- Added `Line` support to `ACHARE` and added `OBJL_0` as a no-op S-57 object
  key for point/line/area metadata.
- Added OpenCPN compatibility lookup rows at source-pack normalization time:
  - `ACHARE / Line` renders with `LC(ACHARE51)`.
  - `OBJL_0 / Line` is modeled as an explicit no-op row.
- Added JVM coverage tests for the new attributes, primitives, and OpenCPN
  compatibility lookup rows.

The `WebGL2 is not available` error remains outside S-52 feature coverage. It is
a consuming-browser/render-context problem and should be fixed on the S-57
snapshot side or with a renderer-context fallback there.
