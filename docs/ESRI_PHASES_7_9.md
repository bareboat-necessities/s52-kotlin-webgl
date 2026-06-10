# ESRI phases 7-9 incremental implementation

This increment builds on the ESRI 0-7 patches and adds the renderer-facing work
needed before full ESRI profile integration.

## Phase ESRI-7 completion

The Lua-to-Kotlin CSP scaffold now has an explicit area-pattern instruction in
addition to symbols, complex lines, simple lines, fills, text, and soundings.
This lets later ports express seabed, restricted-area, and depth-pattern output
without abusing point-symbol commands.

## Phase ESRI-8 complex line rendering

Adds generated vector-line registry support and a WebGL2 complex-line renderer
scaffold. The renderer repeats generated ESRI SVG meshes along a screen-space
polyline and orients each placement to the segment tangent. It consumes generated
Kotlin mesh data; no SVG is loaded at runtime.

## Phase ESRI-9 area pattern rendering

Adds generated vector-pattern registry support and a WebGL2 area-pattern renderer
scaffold. The first implementation tiles generated meshes over a supplied screen
bounding box. It is intentionally structured so polygon clipping/stencil support
can be added without changing the generated vector model.

## New Gradle tasks

```bash
gradle :s52-preslib:generateEsriVectorLines -Pesri.sourceDir=/path/to/nautical-chart-symbols
gradle :s52-preslib:generateEsriVectorPatterns -Pesri.sourceDir=/path/to/nautical-chart-symbols
gradle :s52-preslib:criticalEsriCheck -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

The generated files are:

```text
s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedLineRegistry.kt
s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedPatternRegistry.kt
```

Reports are written under:

```text
s52-preslib/build/reports/esri/generated-vector-lines.json
s52-preslib/build/reports/esri/generated-vector-patterns.json
```

## Current limitations

- Line rendering is screen-space repeat placement. It does not yet split very
  sharp joins into miter/round joins.
- Area-pattern rendering tiles a bounding box. Phase ESRI-10 should connect this
  to the chart renderer's polygon stencil/clip path.
- Robust SVG triangulation is still future work; this increment keeps the simple
  deterministic mesh generator introduced in Phase ESRI-3.
