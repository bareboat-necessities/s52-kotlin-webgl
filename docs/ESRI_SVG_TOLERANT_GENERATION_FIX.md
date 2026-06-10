# ESRI SVG tolerant generation fix

This incremental patch changes Phase ESRI-2/3/8/9 SVG handling from fatal-by-default to tolerant-by-default.

The ESRI `nautical-chart-symbols` repository contains valid SVG assets that are outside the initial Kotlin parser subset. Earlier generator tasks wrote useful partial registries, then returned exit code 1 when any asset failed. That broke CI and atlas generation even though supported symbols had been generated.

Updated behavior:

- `validateEsriSvgSubset` always writes `svg-subset-report.csv/json`.
- `generateEsriVectorSymbols` writes `EsriGeneratedSymbolRegistry.kt` from supported assets and writes `generated-vector-symbols.json` listing unsupported assets.
- `generateEsriVectorLines` and `generateEsriVectorPatterns` do the same for line and pattern registries.
- Unsupported assets are warnings by default, not fatal errors.

To restore strict behavior for parser-completeness or release gates, set:

```bash
ESRI_FAIL_ON_SVG_FAILURES=true
```

Equivalent accepted strict environment flags are:

```bash
ESRI_STRICT_SVG_GENERATION=true
ESRI_STRICT_ESRI_GENERATION=true
```
