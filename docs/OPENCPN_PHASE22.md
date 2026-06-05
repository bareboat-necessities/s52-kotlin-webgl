# Phase 22 — OpenCPN vector symbology import

Phase 22 switches the real symbology export path from the earlier placeholder/libS52-compatible subset to a real **OpenCPN `chartsymbols.xml`** importer.

## What this phase does

- imports scalable/vector symbology from `chartsymbols.xml`
- does **not** use `rastersymbols-*.png`
- exports all imported symbols, line styles, patterns, and colors as SVG images
- fails the build if the imported symbol count is suspiciously small
- changes the project license to **GPL-2.0-or-later** for OpenCPN compatibility

## Required input

Provide OpenCPN `chartsymbols.xml` using either:

```bash
gradle --no-daemon phase22Check -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

or:

```bash
export OPENCPN_CHARTSYMBOLS_XML_FILE=/path/to/chartsymbols.xml
gradle --no-daemon phase22Check
```

## Generated artifact

CI uploads `build/s52-symbology-images` as `opencpn-symbology-images`.
The manifest must contain:

```text
edition=opencpn-chartsymbols-imported
synthetic=false
symbols=<at least 50>
```
