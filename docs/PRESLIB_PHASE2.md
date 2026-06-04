# Phase 2 Presentation Library Pipeline

Phase 2 adds the Presentation Library source/import/generation boundary.

The project still does not bundle official IHO Presentation Library source files. Instead, it provides the structure needed to convert local, externally supplied Presentation Library data into runtime tables.

## Pipeline

```text
external/local Presentation Library source package
        ↓
PresLibSourcePack
        ↓
PresLibValidator.validateSource(...)
        ↓
PresLibKotlinGenerator.generate(...)
        ↓
generated Kotlin source
        ↓
PresLibPackBuilder.build(...)
        ↓
PresLibPack runtime registries
```

## Runtime pack

`PresLibPack` contains:

- `LookupTable`
- `ColorTables`
- `SymbolRegistry`
- `LineStyleRegistry`
- `PatternRegistry`

The portrayal engine consumes these registries and emits renderer-independent draw commands.

## Validation

`PresLibValidator.validate(pack)` reports:

- missing palettes
- missing color tokens
- missing symbols
- missing line styles
- missing patterns
- referenced CSP names
- reference counts

The report can be converted to Markdown with:

```kotlin
val report = PresLibValidator.validate(pack)
println(report.toMarkdown())
```

## Synthetic pack

`GeneratedPhase2PresLib` is a small generated-style fixture. It is not official S-52 content. It contains enough lookup rows and assets to validate the Phase 2 pipeline, including:

- area color lookup
- area pattern lookup
- simple line lookup
- complex line lookup
- point symbol lookup
- text instruction lookup
- CSP reference lookup
- all five S-52 palette buckets

## What Phase 2 intentionally does not do

- It does not parse official IHO source files directly yet.
- It does not claim the bundled synthetic symbols are official.
- It does not implement all CSPs.
- It does not validate CSP registry completeness yet; that is Phase 6.
