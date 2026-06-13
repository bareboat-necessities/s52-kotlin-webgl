# Symbology image export

`GenerateSymbologyImages` and `criticalSymbologyImagesArchive` export the
OpenCPN-compatible portrayal assets into a reviewable directory/ZIP containing:

- `index.html`
- `manifest.properties`
- per-symbol SVG files
- per-line-style SVG files
- per-pattern SVG files
- color swatches
- day, dusk, and dark PNG symbol atlases

The manifest must identify the real imported OpenCPN-compatible payload, not the
synthetic fallback pack. These artifacts are intended for regression review,
issue reports, and release handoff only.
