# Phase 33 WebGL batching and HPGL fill regression fixtures

Phase 33 is based on the OpenCPN baseline committed in `s52/opencpn/chartsymbols.xml` and the embedded `rastersymbols-*.png` atlases.

## Renderer changes

- HPGL `PM`/`FP` polygon paths are compiled into local-space triangle meshes before rendering.
- HPGL `EP` polygon edges, `CI` circles, `AA` arcs, and `RA`/`RR`/`EA`/`ER` rectangles are pre-flattened into explicit fill/stroke geometry.
- `SP` pen changes remain separate display-list geometry batches instead of being merged by pen number. This keeps OpenCPN color references stable when a symbol returns to the same pen later.
- Point raster symbols are batched by atlas texture and alpha.
- Point vector-symbol fills/strokes are batched by color token and stroke width.
- Area patterns are grouped by pattern name, source type, color token, and tile spacing. Clipping remains per feature; the stencil buffer is used only for area fills/patterns, never for point or line symbols.

## Visual regression route

The browser demo includes a deterministic OpenCPN fixture route:

```text
#opencpn-regression
```

It renders:

- a concave polygon with one hole to catch area color leakage,
- HPGL pattern tiles (`MARSHES1`, `FSHFAC04`, `QUESMRK1`) to catch rounded bitmap-preview rectangles,
- high-risk point symbols (`TOPMAR88`, `WRECKS05`, `OBSTRN11`, `LIGHTS11`, `QUESMRK1`) to catch skipped or unresolved symbols.

A helper script can capture this route from an already running demo server:

```bash
node scripts/export-phase33-regression-gallery.mjs http://localhost:8080/#opencpn-regression build/phase33-regression.png
```

Set `CHROME_BIN` if Chromium/Chrome is not on the default path.
