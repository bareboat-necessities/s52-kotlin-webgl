# ESRI source checkout fix

This patch makes the ESRI atlas path robust in GitHub Actions:

- CI/release still check out `Esri/nautical-chart-symbols` into `s52/esri/source`.
- `scripts/prepare-esri-source.sh` verifies the checkout and clones it if missing.
- ESRI JVM generators can auto-fetch the source in GitHub Actions when `s52/esri/source` is absent.
- Set `ESRI_DISABLE_AUTO_FETCH_SOURCE=true` to disable the generator-side fallback.

The generated ESRI Kotlin/WebGL path still uses the real ESRI SVG source tree when available. The fallback only fixes missing CI checkout/order problems; it does not change OpenCPN symbology generation.
