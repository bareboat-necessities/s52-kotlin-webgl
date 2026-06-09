# Phase 22 — OpenCPN vector symbology import

Phase 22 switches the real symbology export path from the earlier placeholder/libS52-compatible subset to an OpenCPN-compatible `chartsymbols.xml` importer plus the bundled raster-symbol atlases.

## What this phase does

- imports scalable/vector symbology from bundled `s52/opencpn/chartsymbols.xml` clean-check payload
- accepts a full upstream OpenCPN `chartsymbols.xml` through the optional override path
- validates that `chartsymbols.xml`, the three `rastersymbols-*.png` atlases, and the committed Kotlin/JS Yarn lock are present before export
- exports all imported symbols, line styles, patterns, and colors as SVG images
- exports three PNG symbol atlases for day, dusk, and dark modes
- fails the build if the imported symbol count is suspiciously small
- fails CI if `kotlin-js-store/yarn.lock` changes during `phase22Check`
- changes the project license to **GPL-2.0-or-later** for OpenCPN compatibility

## Clean-check command

The supported clean-checkout command is the same guard used by CI:

```bash
bash scripts/phase22-clean-check.sh
```

The guard validates bundled OpenCPN-compatible inputs, runs `gradle --no-daemon phase22Check`, and compares the committed Kotlin/JS Yarn lock before and after the Gradle run.

No external `OPENCPN_CHARTSYMBOLS_XML_FILE` is required for the default check path because the repository contains the clean-check payload used by Phase 22.

For a direct Gradle run without the extra preflight and lock-mutation guard:

```bash
gradle --no-daemon phase22Check
```

## Optional external input override

To test against a full upstream OpenCPN `chartsymbols.xml`, use either:

```bash
gradle --no-daemon phase22Check -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

or:

```bash
export OPENCPN_CHARTSYMBOLS_XML_FILE=/path/to/chartsymbols.xml
gradle --no-daemon phase22Check
```

## Generated artifacts

The export task writes to `build/s52-symbology-images` and CI uploads that directory as `opencpn-symbology-images`.

Expected output includes:

- `index.html`
- `manifest.properties`
- `symbols/*.svg`
- `lines/*.svg`
- `patterns/*.svg`
- `colors/*.svg`
- `symbol-atlas-day.png`
- `symbol-atlas-dusk.png`
- `symbol-atlas-dark.png`

The manifest must contain:

```text
edition=opencpn-chartsymbols-imported
synthetic=false
symbols=<at least 50>
pngSymbolAtlases=3
```

## Browser gallery

The default browser demo still loads the s52lib-compatible runtime gallery. Run:

```bash
gradle :demo:jsBrowserDevelopmentRun
```

Then open `#symbols`, `#lines`, `#patterns`, `#colors`, or `#all`.

## Safety and asset boundary

This project remains experimental. It is not type-approved ECDIS and is not for navigation. OpenCPN-derived symbology import and bundled OpenCPN-compatible symbology inputs require the project’s GPL-2.0-or-later licensing boundary.
