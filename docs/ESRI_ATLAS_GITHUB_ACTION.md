# ESRI atlas GitHub Action export

This phase adds an ESRI/INT1 symbology image export path parallel to the existing OpenCPN critical symbology artifact path.

The ESRI export is **OpenCPN-atlas driven**.  It does not simply dump ESRI source SVG names.  It reads `OpenCpnGeneratedPresLib.sourcePack()` and emits the same OpenCPN symbol, line-style, and pattern names, while resolving each slot to the best available ESRI SVG.

The export task consumes a checked-out ESRI `nautical-chart-symbols` tree and produces:

```text
s52-preslib/build/s52-esri-symbology-images/
  index.html
  manifest.properties
  symbols/<OpenCPN symbol name>.svg
  lines/<OpenCPN line style name>.svg
  patterns/<OpenCPN pattern name>.svg
  symbol-atlas-day.png
  symbol-atlas-dusk.png
  symbol-atlas-dark.png
```

The PNG atlases are generated in OpenCPN symbol order, using the same build-time SVG mesh pipeline used by the ESRI Kotlin vector-symbol work.  Runtime rendering remains generated Kotlin vector meshes rendered through WebGL.

Mapping reports are written to:

```text
s52-preslib/build/reports/esri/esri-opencpn-atlas-match.csv
s52-preslib/build/reports/esri/esri-opencpn-atlas-match.json
```

Rows with `CATEGORY_FALLBACK` need better explicit entries in `s52/esri/esri-symbol-aliases.tsv`, `s52/esri/esri-line-aliases.tsv`, or `s52/esri/esri-pattern-aliases.tsv`.

## Local run

```bash
gradle :s52-preslib:criticalEsriSymbologyImagesArchive \
  -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

or:

```bash
export ESRI_NAUTICAL_CHART_SYMBOLS_DIR=/path/to/nautical-chart-symbols
gradle :s52-preslib:criticalEsriSymbologyImagesArchive
```

The archive is written under:

```text
s52-preslib/build/distributions/*esri-symbology-images*.zip
```

## GitHub Actions

The CI and release workflows now check out:

```text
Esri/nautical-chart-symbols -> s52/esri/source
```

Then they run the ESRI critical check and build/upload the ESRI symbology atlas ZIP artifact. The release workflow also includes the ESRI atlas ZIP in GitHub Release assets.
