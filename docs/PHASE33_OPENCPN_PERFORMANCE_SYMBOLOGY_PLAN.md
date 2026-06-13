# Phase 33: OpenCPN Symbology Completion and Performance Plan

This phase targets renderer-visible gaps in OpenCPN symbology support while keeping the WebGL hot path bounded and batch-friendly.

## Immediate fixes included in this phase

1. **Area color fills must be clipped by a real polygon mask.**
   * Use a triangulated stencil mask instead of triangle-fan ring toggles. Triangle fans are fast, but they are incorrect for many concave S-57 rings and can invert or fill outside the intended shape.
   * Keep the existing scissor rectangle so the stencil clear, mask write, and final fill touch only the projected polygon bounds.
   * Reset stencil/scissor/color-write state at frame start and always restore it after a clip pass, including zero-triangle edge cases.

2. **OpenCPN area patterns should prefer vector HPGL over raster atlas previews.**
   * Many OpenCPN pattern records have both HPGL and a bitmap cell. The bitmap can be a catalogue preview or placeholder with visible rounded box edges.
   * Render HPGL first and fall back to raster only when no vector pattern is available.

3. **Raster symbol/pattern sampling should avoid neighbouring atlas texels.**
   * Use half-texel-inset UVs for raster symbols, matching the pattern renderer, to avoid color streaks from tightly packed OpenCPN atlases.
   * Continue using `NEAREST` filtering for all OpenCPN atlas textures.

4. **Renderer-side symbol recovery should avoid silent skips.**
   * Resolve known project-level CSP symbol names to OpenCPN chart-symbol names where the two naming schemes differ.
   * Cache parsed symbol HPGL segment lists so repeated render frames do not repeatedly parse the same symbol source.

## Next implementation steps

### 1. Build a complete OpenCPN asset coverage index

* Generate a normalized symbol, line, pattern, color, lookup, and CSP reference table from `chartsymbols.xml`, `s57objectclasses.csv`, and the generated Kotlin pack.
* Add a strict CI check that fails on newly unresolved references, but allow an explicit compatibility-alias table for deliberate project/OpenCPN naming differences.
* Store counts by asset class and primitive type so regressions are visible without opening a browser.

### 2. Replace line-only HPGL rendering with a compiled vector-display-list model

* Parse HPGL into a compact intermediate form: pen selection, stroke width, move/line/arc/circle, polygon-mode boundaries, and fill commands.
* Compile each OpenCPN asset once per presentation library into immutable stroke and fill meshes.
* Keep per-frame work to transform, append, and draw precompiled vertices; do not tokenize HPGL during rendering.

### 3. Add fill support for HPGL symbols and patterns

* Convert `PM`/`FP` polygon-mode paths into triangulated local-space meshes.
* Preserve `SP` color transitions as separate mesh batches keyed by color token.
* Support circles/arcs as pre-flattened polylines/triangle fans with a bounded segment budget.

### 4. Batch OpenCPN symbols and patterns by GPU state

* Batch raster point symbols by atlas texture and alpha.
* Batch vector symbol strokes/fills by color token and line width.
* Batch area patterns by pattern name, color token, and tile spacing where polygons share the same viewport transform.
* Keep per-feature clipping isolated; use stencil only for area patterns/fills and avoid stencil for point/line symbols.

### 5. Add visual regression fixtures for the failure modes

* Add a synthetic concave polygon with a hole to catch color leakage.
* Add a pattern tile fixture that verifies HPGL patterns render without rounded bitmap-preview rectangles.
* Add topmark, wreck, obstruction, light, and question-mark symbols to catch skipped or unresolved symbols.
* Export a small deterministic PNG gallery from the browser demo or a headless WebGL harness for before/after diffing.

### 6. Performance targets

* No HPGL parsing in steady-state render frames.
* One texture upload per palette atlas, cached across renderer instances.
* O(number of visible tiles) pattern vertex generation with viewport-bounded row/column caps.
* Area fills/patterns limited by scissor rectangles; no full-canvas stencil clears per feature.
* Track draw-call count, batch count, average commands per batch, and generated vertex counts in renderer stats.

## Open risks

* Some OpenCPN HPGL constructs are still parsed only as line geometry. Filled symbol parity requires the compiled display-list work above.
* Browser screenshots are required for final verification of visual parity; JVM tests can validate coverage and command generation but cannot prove atlas/pattern appearance.
* Stencil triangulation improves correctness but adds CPU work. The compiled/cached polygon-mask plan should revisit reuse for static ENC tiles.
