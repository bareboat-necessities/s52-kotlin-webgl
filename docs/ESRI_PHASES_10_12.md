# ESRI phases 10-12

This increment completes the first end-to-end ESRI profile integration path.

## Phase ESRI-10: `EsriInt1` profile facade

Phase 10 adds a runtime-neutral ESRI profile facade that aggregates:

- generated ESRI point-symbol meshes,
- generated ESRI complex-line meshes,
- generated ESRI area-pattern meshes,
- generated direct rules from `CustomSymbolMap.xml`,
- high-priority Kotlin CSP ports,
- alias-table coverage metadata.

The facade is intentionally lightweight. It does not replace the existing S-52
portrayal session yet; instead, it gives downstream API/demo code one stable
entry point while the remaining ESRI adapters are wired in.

## Phase ESRI-11: strict coverage closure

Phase 11 adds a strict release gate. The gate reads the OpenCPN-generated
coverage oracle and the ESRI generated/alias outputs and writes:

```text
s52-preslib/build/reports/esri/strict-coverage.json
s52-preslib/build/reports/esri/missing-esri-coverage.csv
```

Release mode fails if unresolved ESRI coverage remains. Development mode can
still emit the report without failing.

## Phase ESRI-12: NOAA smoke harness

Phase 12 adds a small NOAA-style smoke harness. It is not a full S-57 parser.
The harness consumes synthetic/fixture feature rows that resemble decoded NOAA
feature records and verifies that the ESRI profile can produce non-empty rule or
CSP results for important feature classes.

A later chartplotter integration phase should replace this fixture harness with
real S-57/SENC decoded features from the ISO8211/S-57 project.

## New tasks

```bash
gradle :s52-preslib:generateEsriPresLib -Pesri.sourceDir=/path/to/nautical-chart-symbols
gradle :s52-preslib:checkEsriStrictCoverage -Pesri.sourceDir=/path/to/nautical-chart-symbols
gradle :s52-preslib:esriNoaaSmokeTest -Pesri.sourceDir=/path/to/nautical-chart-symbols
gradle :s52-preslib:criticalEsriCheck -Pesri.sourceDir=/path/to/nautical-chart-symbols
```

`criticalEsriCheck` now covers ESRI phases 0 through 12.
