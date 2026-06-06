# Phase 30 — OpenCPN raster/vector symbol rendering

Phase 30 is the first WebGL rendering patch that consumes the OpenCPN assets
made available by phases 28A/28B/28C and the OpenCPN lookup behavior from phase
29.

## Scope

This phase intentionally focuses on point symbols (`SY(...)`). Complex line
styles (`LC(...)`) and area patterns (`AP(...)`) remain for the next phase.

Implemented:

- WebGL textured-quad program for raster-symbol atlases.
- Lazy browser loading of OpenCPN raster atlases from `s52/opencpn/`.
- Palette-to-atlas selection:
  - `DayBright`, `DayBlackBack`, `DayWhiteBack` -> `rastersymbols-day.png`
  - `Dusk` -> `rastersymbols-dusk.png`
  - `Night` -> `rastersymbols-dark.png`
- Bitmap symbol rendering from `RasterBitmapDefinition` atlas coordinates.
- Symbol pivot and rotation support for raster symbols.
- HPGL line-extraction fallback for vector-only symbols and while raster atlas
  images are still loading.
- OpenCPN demo gallery routes:
  - `#opencpn-symbols`
  - `#opencpn-lines`
  - `#opencpn-patterns`
  - `#opencpn-colors`

## Resource layout

The three OpenCPN raster atlases are copied into browser resources under:

```text
s52/opencpn/rastersymbols-day.png
s52/opencpn/rastersymbols-dusk.png
s52/opencpn/rastersymbols-dark.png
```

The renderer defaults to that same relative path, so a consuming browser app can
either use the bundled resources or serve equivalent files at the same URL.

## Deliberate limitations

The HPGL fallback in this phase extracts line geometry only from common commands
such as `PU`, `PD`, `CI`, and `AA`. It does not yet preserve full HPGL styling,
filled polygons, pen colors, or dash patterns. That richer vector renderer is
planned with the complex line/pattern work.
