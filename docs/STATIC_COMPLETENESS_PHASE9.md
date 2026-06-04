# Phase 9 — Static completeness tests

Phase 9 makes Presentation Library completeness measurable. The project still
uses the synthetic generated-style Presentation Library fixture, but the checks
are the same shape needed for a full imported S-52 Presentation Library pack.

## What is validated

`StaticCompletenessValidator` checks both source packs and runtime packs:

- every lookup instruction parses;
- every referenced `SY(...)` symbol exists;
- every referenced `LC(...)` line style exists;
- every referenced `AP(...)` pattern exists;
- every referenced `AC(...)` / `LS(...)` color token exists in each required palette;
- every referenced `CS(...)` procedure is implemented when a CSP registry/name set is supplied;
- every lookup row uses a primitive supported by its typed `S57ObjectClass`;
- source-side duplicate asset names are reported;
- source-side color RGB values are checked for the `0..255` range;
- source-side attribute filters are checked against coarse `S57AttributeValueKind` metadata.

## Main API

```kotlin
val report = StaticCompletenessValidator.validatePack(
    pack = GeneratedPhase2PresLib.pack(),
    implementedCsps = CspId.completePhase6Names()
)

check(!report.hasErrors) { report.toMarkdown() }
```

For importer/generator tests, validate the source pack before building the
runtime pack:

```kotlin
val sourceReport = StaticCompletenessValidator.validateSource(
    source = GeneratedPhase2PresLib.sourcePack(),
    implementedCsps = CspId.completePhase6Names()
)
```

## Report format

The validator returns a structured `StaticCompletenessReport` and can also emit a
Markdown summary through `toMarkdown()`:

```text
# Static Completeness Report

- Lookup records: 22
- Parsed instructions: 22
- Symbols: 15
- Line styles: 5
- Patterns: 12
- Palettes: 5
- Referenced symbols: 2
- Referenced line styles: 1
- Referenced patterns: 0
- Referenced colors: 1
- Referenced CSPs: 16
- Implemented CSPs: 16
- Diagnostics: 0
```

## Boundary

Phase 9 does not claim official IHO Presentation Library completeness because
the official assets are not bundled. It proves that the project can enforce
completeness once a local/imported Presentation Library source package is
provided.
