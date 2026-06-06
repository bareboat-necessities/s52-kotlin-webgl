# Phase 28A — OpenCPN asset inventory and raw parser foundation

Phase 28A adds a non-runtime inventory layer for the corrected OpenCPN portrayal payload under `s52/opencpn`.

This phase deliberately does **not** replace the existing runtime Presentation Library, lookup logic, or WebGL rendering path. It only verifies that the repository contains a complete OpenCPN payload and exposes raw parsed data for later phases.

## Added parser coverage

- `chartsymbols.xml`
  - color tables
  - lookup count
  - symbols
  - line styles
  - patterns
  - bitmap geometry and atlas coordinates
  - vector metadata and raw HPGL strings
- companion CSV files
  - `s57objectclasses.csv`
  - `s57attributes.csv`
  - `s57expectedinput.csv`
  - `attdecode.csv`
- raster atlases
  - `rastersymbols-day.png`
  - `rastersymbols-dusk.png`
  - `rastersymbols-dark.png`

## Verification task

Run:

```bash
./gradlew :s52-preslib:openCpnInventory
```

Expected corrected-baseline inventory:

```text
colorTables=5
colors=315
lookups=3057
symbols=1093
lineStyles=57
patterns=30
symbolBitmap=1083
symbolVector=375
lineStyleVector=57
patternBitmap=8
patternVector=25
rasterAtlases=3, all 1500x1200
```

## Next phase

Phase 28B should consume this raw inventory layer to introduce dynamic S-57 object/attribute keys and import all 3057 OpenCPN lookup records without dropping rows that use OpenCPN/private object-class names.
