# ESRI OpenCPN-matched atlas contract

The ESRI symbology-image artifact is intentionally driven by the OpenCPN generated Presentation Library, not by the raw ESRI SVG inventory.

The exporter reads `OpenCpnGeneratedPresLib.sourcePack()` and emits:

- `symbols/<OpenCPN symbol name>.svg`
- `lines/<OpenCPN line style name>.svg`
- `patterns/<OpenCPN pattern name>.svg`
- `symbol-atlas-day.png`
- `symbol-atlas-dusk.png`
- `symbol-atlas-dark.png`

For every OpenCPN slot, the drawing source is resolved to an ESRI SVG by this order:

1. explicit alias TSV under `s52/esri/`,
2. direct `CustomSymbolMap.xml` object rule,
3. exact ESRI SVG file/base-name match,
4. semantic token match,
5. category fallback.

The output names and counts are therefore comparable 1:1 with the OpenCPN atlas, while the visual source is ESRI/INT1.  Mapping quality is reported in:

```text
s52-preslib/build/reports/esri/esri-opencpn-atlas-match.csv
s52-preslib/build/reports/esri/esri-opencpn-atlas-match.json
```

Any row with `CATEGORY_FALLBACK` should be closed later by adding a better alias to `s52/esri/esri-symbol-aliases.tsv`, `s52/esri/esri-line-aliases.tsv`, or `s52/esri/esri-pattern-aliases.tsv`.
