# ESRI phases 0-2 implementation notes

This patch adds the first implementation slice for the ESRI/INT1 symbology effort.

## Phase ESRI-0

Added source and legal-boundary documentation plus source revision metadata.

## Phase ESRI-1

Added JVM import tooling that inventories:

- `CustomPresentationLibrary/CustomSymbolMap.xml`
- `CustomPresentationLibrary/lua/*.lua`
- `CustomPresentationLibrary/symbols/{point,line,pattern}/*.svg`

It also writes an initial OpenCPN coverage/gap report. The report uses the generated OpenCPN pack as the coverage oracle and compares direct ESRI object rules as an early, conservative signal. Later ESRI phases should replace this with full lookup/CSP/symbol/line/pattern closure.

## Phase ESRI-2

Added a JVM SVG parser/subset validator. This phase records SVG metadata and validates the initial supported SVG subset. It does not generate runtime Kotlin mesh code yet; that starts in ESRI-V3.

## Run

```bash
gradle :s52-preslib:criticalEsriCheck -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

Reports are written to:

```text
s52-preslib/build/reports/esri/
```
