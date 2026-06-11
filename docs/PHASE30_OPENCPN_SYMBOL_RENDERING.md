# Phase 30 — OpenCPN raster/vector symbol rendering

Phase 30 is the first WebGL rendering patch that consumes the OpenCPN assets
made available by phases 28A/28B/28C and the OpenCPN lookup behavior from phase
29.

## Scope

This phase intentionally focuses on point symbols (`SY(...)`). Complex line
styles (`LC(...)`) and area patterns (`AP(...)`) remain for the next phase.

Implemented:

- WebGL textured-quad program for raster-symbol atlases.
- Lazy browser loading of OpenCPN raster atlases from generated Kotlin data URIs.
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

The three OpenCPN raster atlases are source inputs under root `s52/opencpn`, but they are not browser runtime resources.  `:s52-preslib:generateOpenCpnRasterAtlasData` embeds them into generated commonMain Kotlin as chunked base64 `data:image/png` URIs before compilation.

A consuming browser application using `s52-render-webgl` only needs the compiled Kotlin/JS library output.  It does not need to copy or serve `rastersymbols-*.png` beside its own `index.html`.

## Deliberate limitations

The HPGL fallback in this phase extracts line geometry only from common commands
such as `PU`, `PD`, `CI`, and `AA`. It does not yet preserve full HPGL styling,
filled polygons, pen colors, or dash patterns. That richer vector renderer is
planned with the complex line/pattern work.
