# Phase 23 — OpenCPN importer compile fix

Phase 23 fixes the JVM compile failure introduced by the Phase 22 OpenCPN
`chartsymbols.xml` importer.

## Fixed

The previous importer source could contain broken Kotlin character literals for
newline/control-character checks. Once that happened, helper functions such as
`looksLikeSymbolName`, `parseHpglLikeVector`, `parseCoordinatePairs`,
`parseNumbers`, `circleApprox`, `arcApprox`, and `fallbackSymbolCommands` were
parsed incorrectly or became unreachable, causing `:s52-preslib:compileKotlinJvm`
to fail.

Phase 23 replaces the importer with a JVM-safe implementation that:

- avoids fragile literal newline character expressions
- avoids destructuring ambiguous inferred values in HPGL parsing
- supports XML `chartsymbols.xml` input
- supports legacy flat HPGL-like OpenCPN symbol streams
- keeps raster atlas use disabled

## Build

```bash
gradle --no-daemon phase22Check -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

or through CI using `OPENCPN_CHARTSYMBOLS_XML_BASE64`.
