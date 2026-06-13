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

## Completed implementation steps

### 1. Build a complete OpenCPN asset coverage index

* Added `S52OpenCpnDiagnostics.coverageIndex()` with normalized symbol, line, pattern, color, lookup, and CSP coverage from the generated runtime pack.
* The index resolves deliberate project/OpenCPN symbol-name differences through an explicit compatibility-alias table.
* The index stores asset-class counts, HPGL display-list/fill capability counts, primitive lookup counts, presentation-table counts, and unresolved reference sets for CI use.

### 2. Replace line-only HPGL rendering with a compiled vector-display-list model

* Added a WebGL `HpglDisplayListCompiler` that parses HPGL into reusable pen-keyed stroke and fill geometry.
* Symbols, line styles, and area patterns cache compiled display lists by asset name.
* Rendering transforms precompiled geometry each frame instead of reparsing HPGL strings in the hot path.

### 3. Add fill support for HPGL symbols and patterns

* `PM`/`FP` polygon-mode paths are converted into triangulated local-space fill meshes.
* `SP` pen transitions are preserved as separate geometry batches and mapped back to OpenCPN color references.
* Circles/arcs are pre-flattened into bounded line geometry; rectangle fill/edge commands are compiled as fill/stroke geometry.

## Remaining implementation steps

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

* Some OpenCPN HPGL constructs beyond the compiled subset may still need specialized support after visual comparison with OpenCPN.
* Browser screenshots are required for final verification of visual parity; JVM tests can validate coverage and command generation but cannot prove atlas/pattern appearance.
* Stencil triangulation improves correctness but adds CPU work. The compiled/cached polygon-mask plan should revisit reuse for static ENC tiles.
