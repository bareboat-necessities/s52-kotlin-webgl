# ESRI phases 2-4 implementation notes

This increment extends the ESRI proof-of-life importer from phases 0-2 into the
first vector rendering pipeline.

## Phase ESRI-2: SVG parser completion

The SVG parser now feeds a path flattener that understands the initial supported
subset:

- `M`, `L`, `H`, `V`, `C`, `S`, `Q`, `T`, `Z`
- absolute and relative coordinates
- inline `fill`, `stroke`, `stroke-width`, and `style` values
- `width`, `height`, and `viewBox`

Unsupported commands and unsupported SVG features still fail validation rather
than rendering with a placeholder.

## Phase ESRI-3: Kotlin vector mesh generation

The new task:

```bash
gradle :s52-preslib:generateEsriVectorSymbols -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

reads ESRI SVG files and writes:

```text
s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedSymbolRegistry.kt
```

It also writes:

```text
s52-preslib/build/reports/esri/generated-vector-symbols.json
```

The generated file contains Kotlin data, not SVG strings.  Filled paths are
converted to triangle fans, and stroked paths are converted to rectangular
triangle strips.  This is intentionally conservative; later phases can replace
the tessellator without changing the generated runtime model.

## Phase ESRI-4: WebGL vector symbol renderer scaffolding

`s52-render-webgl` now has an ESRI vector symbol renderer that consumes generated
`EsriVectorSymbol` mesh data and draws triangles through WebGL.  It does not load
or parse SVG at runtime.

The renderer expects a minimal shader contract:

```glsl
attribute vec2 a_position;
uniform vec4 u_color;
```

The calling chart renderer is responsible for selecting the ESRI profile,
resolving `PointSymbol.symbolName`, and invoking the ESRI renderer for generated
ESRI symbols.

## Updated critical task

```bash
gradle :s52-preslib:criticalEsriCheck -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

now runs inventory, coverage report, SVG subset validation, vector Kotlin
generation, and JVM tests.
