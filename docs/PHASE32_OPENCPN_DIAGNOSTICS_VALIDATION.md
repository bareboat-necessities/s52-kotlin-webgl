# Phase 32 — OpenCPN diagnostics, demos, and validation

Phase 32 makes the OpenCPN Presentation Library integration inspectable without
changing the Phase 30/31 rendering path.

## Added browser routes

The demo keeps the existing gallery routes and adds two diagnostics routes:

- `#opencpn-symbols`
- `#opencpn-lines`
- `#opencpn-patterns`
- `#opencpn-colors`
- `#opencpn-lookups`
- `#opencpn-diagnostics`

`#opencpn-lookups` prints lookup table counts, primitive counts, presentation
subtable counts, and reference totals.

`#opencpn-diagnostics` prints a full renderer-independent OpenCPN coverage
summary and also draws the first lines of the report on the canvas using the
existing vector text renderer.

## Runtime diagnostics API

`S52OpenCpnDiagnostics.report()` returns an `OpenCpnDiagnosticsReport` with:

- lookup, symbol, line-style, pattern, and color-table counts
- raster/vector asset counts
- color counts per palette
- display-category, primitive, and OpenCPN presentation-table counts
- referenced symbols, line styles, patterns, colors, and CSP names
- unresolved symbol/line/pattern/color/CSP references
- HPGL command mnemonics outside the currently supported renderer subset
- known raster atlas file names

This report is intentionally independent of WebGL. It can be used from tests,
CLI tools, browser status panels, and future chart-loader integration.

## Validation added

Phase 32 adds JVM tests for:

- generated OpenCPN payload counts exposed through diagnostics
- text rendering of the diagnostics report
- representative OpenCPN feature portrayal smoke coverage

The representative feature smoke test is deliberately broad rather than pixel
based. Pixel/golden-image validation should come after the S-57 chart ingestion
layer supplies real NOAA feature payloads and after the OpenCPN renderer is more
complete.
