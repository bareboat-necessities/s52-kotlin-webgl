# Phase 29 — OpenCPN lookup matching and CSP behavior

Phase 29 is intentionally still renderer-neutral. It does not attempt to draw
OpenCPN raster symbols, vector HPGL, complex line tiles, or area pattern tiles.
That remains Phase 30/31 work.

This phase makes the generated OpenCPN Presentation Library pack usable by the
core portrayal engine:

- OpenCPN `table-name` values are preserved on runtime lookup rows.
- Point lookups are selected from `Simplified` or `Paper` according to
  `MarinerSettings.symbolStyle`.
- Area lookups are selected from `Plain` or `Symbolized` according to
  `MarinerSettings.boundaryStyle`.
- Line lookups from the `Lines` table are accepted for line primitives.
- Raw OpenCPN `attrib-code` filters are evaluated in commonMain, including
  forms such as `CATACH8`, `COLOUR3,1`, `DRVAL1?`, `CONDTN`, `fnctnm5`, and
  `cattml3`.
- Attribute matching now works with dynamic `S57AttributeKey` values as well as
  the existing enum attributes.
- `S57Attributes` can carry a key-based map for OpenCPN/private attributes
  while preserving the old enum-based API.
- `DefaultCspRegistry.openCpn()` registers the OpenCPN-numbered CSP names
  referenced by `chartsymbols.xml`.
- `S52Runtime.openCpn()` and `S52PortrayalSession.openCpn()` now use the
  OpenCPN CSP registry instead of the synthetic Phase 6 registry.

The current OpenCPN CSP registry intentionally aliases the already implemented
core CSPs where safe. A small number of OpenCPN/mariner-overlay CSP names are
registered with conservative placeholder behavior so they no longer crash the
engine during lookup expansion. Their visual behavior should be improved during
the renderer/demos phases.

Run the new verification target with:

```bash
./gradlew phase29Check
```

The execution environment used to create this incremental patch did not include
`gradlew` or a system Gradle installation, so full Gradle execution could not be
performed here.
