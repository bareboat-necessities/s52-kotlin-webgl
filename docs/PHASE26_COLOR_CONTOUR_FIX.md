# Phase 26 — OpenCPN SVG color, contour, bounds, and line-style export fix

Phase 26 fixes the generated SVG inspection artifacts for OpenCPN `chartsymbols.xml`.

## Problem

The previous exporter flattened HPGL into a single black stroked path. That lost:

- `color-ref` entries
- `SP` pen/color selection
- `SW` stroke-width changes
- `LT` line-type/dash hints
- polygon contour state from `PM`
- filled/outlined polygon commands from `FP` and `EP`
- stroke-width-aware SVG bounds
- line-style repetition semantics

The result was recognizable but incomplete: symbols were mostly black line art, some contours looked wrong, some strokes could be clipped by the SVG bounds, and line-style artifacts did not show the style repeated along a sample line.

## Fix

The OpenCPN importer keeps a renderable HPGL asset model alongside the normal `PresLibSourcePack`. The SVG exporter uses that renderable model to emit multiple SVG elements with proper colors, contours, bounds, and repeated line-style samples.

Supported HPGL state in the SVG exporter:

- `SP` selects colors through the asset `color-ref` list
- `SW` changes stroke width
- `LT` maps common line types to SVG dash arrays
- `PU` starts a new contour/subpath
- `PD` draws lines
- `CI` draws circles
- `AA` now uses HPGL center-point arc semantics
- `PM` enters/closes/exits polygon mode
- `FP` emits filled polygon contours
- `EP` emits outlined polygon contours
- `EA` / `RA` / `ER` / `RR` draw or fill rectangles

Bounds are computed from the emitted geometry and expanded by stroke width before the SVG viewBox is generated. Line-style SVGs are now repeated samples using the imported line-style HPGL, not a generic placeholder line.

The output manifest now includes:

```text
svgColorAware=true
svgContourAware=true
svgBoundsAware=true
lineStyleSampleRepeated=true
hpglArcCenterAware=true
```

Raster atlases remain intentionally unused.
