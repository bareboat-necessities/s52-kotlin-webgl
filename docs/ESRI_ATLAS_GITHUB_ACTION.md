# ESRI atlas GitHub Action export

This phase adds an ESRI/INT1 symbology image export path parallel to the existing OpenCPN critical symbology artifact path.

The export task consumes a checked-out ESRI `nautical-chart-symbols` tree and produces:

```text
s52-preslib/build/s52-esri-symbology-images/
  index.html
  manifest.properties
  symbols/*.svg
  lines/*.svg
  patterns/*.svg
  symbol-atlas-day.png
  symbol-atlas-dusk.png
  symbol-atlas-dark.png
```

The PNG atlases are generated from the same build-time SVG mesh pipeline used by the ESRI Kotlin vector-symbol work. They are not the primary runtime path. Runtime rendering remains generated Kotlin vector meshes rendered through WebGL.

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
