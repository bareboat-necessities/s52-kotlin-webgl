# ESRI source workspace

This directory holds project-owned ESRI integration metadata and optional local-only source inputs.

Recommended local layout:

```text
s52/esri/
  source-revision.properties
  source/                         # optional local checkout, do not commit full upstream tree by default
    CustomPresentationLibrary/
      CustomSymbolMap.xml
      lua/
      symbols/
        point/
        line/
        pattern/
```

Run with an explicit external source path when possible:

```bash
gradle :s52-preslib:criticalEsriCheck -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

or:

```bash
export ESRI_NAUTICAL_CHART_SYMBOLS_DIR=/path/to/nautical-chart-symbols
gradle :s52-preslib:criticalEsriCheck
```
