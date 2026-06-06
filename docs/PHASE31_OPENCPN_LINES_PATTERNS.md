# Phase 31 — OpenCPN complex lines and area patterns

Phase 31 builds on Phase 30's OpenCPN raster-symbol atlas support and upgrades the WebGL rendering path for `LC(...)` and `AP(...)` commands.

## Included

- `LineStyleDefinition` and `PatternDefinition` now preserve OpenCPN pivot/width/height metadata already present in `chartsymbols.xml`.
- `SourceLineStyle` and `SourcePattern` carry the same dimensions through generated source-pack creation.
- `OpenCpnGeneratedPresLib` now maps compact line-style/pattern rows into those runtime dimensions.
- `LineRenderer.renderComplex()` no longer draws complex lines as a plain black `LINE_STRIP` when an OpenCPN line-style HPGL asset is available.
- Complex line rendering now repeats the imported OpenCPN line-style HPGL geometry along the feature line.
- `AreaPatternRenderer` now resolves OpenCPN pattern assets by name and tiles either:
  - raster atlas pattern cells, when a bitmap ref is available and the atlas texture has loaded; or
  - HPGL vector line geometry, when vector HPGL is available.
- The old generic hatch renderer remains only as a fallback for missing/unavailable pattern assets.

## Intentional limits

This phase is still a pragmatic geometry renderer, not the final full styled HPGL renderer:

- HPGL `SP`/`SW`/`LT` pen styling is not yet interpreted command-by-command.
- Pattern clipping uses tile-center inclusion against the polygon outer ring.
- Polygon holes are not handled yet.
- Thick strokes are still WebGL line primitives, not triangulated stroke meshes.

Those limitations are acceptable for Phase 31 because the important behavior change is that OpenCPN `LC(...)` and `AP(...)` now render imported assets rather than synthetic black lines and generic hatch placeholders.

## Manual demo routes

Use the existing browser demo routes:

```text
#opencpn-lines
#opencpn-patterns
#opencpn-chart
```

Expected result:

- `#opencpn-lines` shows repeated OpenCPN line-style glyphs along sample lines.
- `#opencpn-patterns` shows OpenCPN pattern tiles instead of the old generic hatch-only rendering.
- `#opencpn-chart` can draw area-boundary `LC(...)` and area `AP(...)` commands with OpenCPN assets when those commands are selected by lookup/CSP logic.
