# ESRI nautical chart symbols source

This project can generate an experimental ESRI/INT1-style symbology profile from the public ESRI nautical chart symbols repository.

Source repository:

```text
https://github.com/Esri/nautical-chart-symbols
```

Expected source layout after checkout or archive extraction:

```text
<esri-source-root>/
  CustomPresentationLibrary/
    CustomSymbolMap.xml
    lua/
    symbols/
      point/
      line/
      pattern/
```

The ESRI source tree is intentionally treated as an external input. Do not commit a full third-party checkout unless the project license and release policy explicitly allow it. Use one of these local input paths instead:

```bash
gradle :s52-preslib:esriInventory -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

or:

```bash
export ESRI_NAUTICAL_CHART_SYMBOLS_DIR=/path/to/nautical-chart-symbols
gradle :s52-preslib:esriInventory
```

For convenience during local development, a source checkout may also be placed under:

```text
s52/esri/source/
```

That directory should remain ignored by release tooling unless a later phase deliberately vendors a curated subset with the required notices.

## Phase ESRI-0/1/2 boundary

The initial ESRI integration phases do not change the runtime portrayal profile yet. They add:

- source provenance metadata under `s52/esri/source-revision.properties`
- legal/source-boundary documentation
- ESRI source inventory tooling
- OpenCPN-required-coverage report scaffolding
- SVG subset parsing and validation tooling

The OpenCPN-compatible generated pack remains the coverage oracle for the eventual ESRI profile.

## Commands

Inventory and initial gap report:

```bash
gradle :s52-preslib:esriInventory -Pesri.sourceDir=/path/to/nautical-chart-symbols
gradle :s52-preslib:esriCoverageReport -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

SVG subset validation:

```bash
gradle :s52-preslib:validateEsriSvgSubset -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

Combined phase check:

```bash
gradle :s52-preslib:criticalEsriCheck -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

Generated reports are written to:

```text
s52-preslib/build/reports/esri/
```

## Enhanced OpenCPN-compatible SVG handoff

The ESRI image export now produces an intermediate `enhanced-svg/` directory under `s52-preslib/build/s52-esri-symbology-images/`.  This set is intended to be the portrayal-input handoff before Kotlin vector generation:

- it is keyed by OpenCPN symbol, line, pattern, and lookup-object names;
- resolved files retain ESRI SVG geometry but recolor monochrome artwork with OpenCPN/S-52-inspired day colors;
- each file carries `data-opencpn-name`, match metadata, and deterministic category/identity overlays so repeated ESRI source shapes remain visually distinct;
- unresolved matches are explicit visible review placeholders rather than silent generic substitutes.

CI and release workflows upload this directory separately as the `esri-enhanced-svg-portrayal-input` artifact, in addition to the full ESRI symbology-image artifact.
