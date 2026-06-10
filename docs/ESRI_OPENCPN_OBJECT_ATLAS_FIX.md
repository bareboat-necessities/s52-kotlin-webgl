# ESRI OpenCPN object atlas fix

This incremental fix tightens the ESRI atlas contract so the export is driven by
OpenCPN coverage, not by the incidental list of SVG files found in the ESRI
repository.

The ESRI image export now emits all three OpenCPN coverage layers:

- `symbols/`, `lines/`, and `patterns/` keep the OpenCPN asset names and resolve
  each slot to the best available ESRI presentation SVG.
- `objects/` is generated from `OpenCpnGeneratedPresLib.sourcePack().lookupRecords`
  and therefore follows the same S-57 object acronyms that OpenCPN supports.
- `manifest.properties` and `esri-opencpn-atlas-match.json` now report the
  OpenCPN lookup object count as `objects` / `opencpnObjectCount`.
- `esri-opencpn-object-match.csv` records which OpenCPN lookup instruction asset
  was used for each object and which ESRI source SVG was selected.

Run:

```bash
gradle :s52-preslib:criticalEsriSymbologyImagesArchive \
  -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

The resulting ZIP contains `s52-esri-symbology-images/objects/*.svg` alongside the
OpenCPN-name-compatible symbol/line/pattern SVGs and the three symbol PNG atlases.
