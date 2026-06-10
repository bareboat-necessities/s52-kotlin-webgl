# ESRI symbology license boundary

The ESRI nautical chart symbols source is an external third-party input for the experimental ESRI/INT1-style profile.

The intended source is:

```text
https://github.com/Esri/nautical-chart-symbols
```

The ESRI repository declares Apache License, Version 2.0 licensing. Keep the upstream license and notice files together with any vendored subset or generated release artifact that includes ESRI-derived data.

This repository also has an OpenCPN-compatible symbology import path whose README documents a GPL-2.0-or-later compatibility boundary. The ESRI integration must not blur those sources:

- OpenCPN-derived data remains under the existing OpenCPN/GPL-compatible boundary.
- ESRI-derived data remains under the ESRI/Apache-2.0 source boundary.
- Generated reports should identify which source produced each asset or rule.
- Generated Kotlin in later ESRI phases should include provenance comments that reference the ESRI source revision.

## What Phase ESRI-0/1/2 commits

These phases commit only project-owned tooling and metadata:

```text
docs/ESRI_SYMBOLS_SOURCE.md
docs/ESRI_LICENSE_BOUNDARY.md
s52/esri/source-revision.properties
s52/esri/README.md
s52-preslib/src/jvmMain/kotlin/.../esri/importer/*.kt
s52-preslib/src/jvmMain/kotlin/.../esri/svg/*.kt
```

They do not commit the full ESRI source tree.

## Future generated artifacts

Later phases that generate Kotlin vector meshes from ESRI SVGs should include a generated header like:

```text
Generated from ESRI nautical-chart-symbols, commit <sha>, CustomPresentationLibrary/symbols/... .
Source license: Apache-2.0. See docs/ESRI_LICENSE_BOUNDARY.md.
```

If release artifacts include ESRI-derived SVG previews, PNG previews, or generated Kotlin mesh data, they must include the applicable ESRI license and notice metadata.
