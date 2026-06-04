# Phase 1 Catalogue Boundary

Phase 1 finishes the typed import boundary for normalized ENC-like features.

The catalogue files are generated-style Kotlin enums, even though they are still curated by hand in this phase. They are intentionally data-only and can be replaced by generated output later without changing the public API.

## What is typed now

- `S57ObjectClass`
- `S57Attribute`
- `S57AttributeValueKind`
- `S57EnumeratedValue`
- `PrimitiveType`

## What stays outside the enum classes

No drawing logic is stored on object classes or attributes. S-52 behavior belongs to:

- lookup records
- instruction evaluation
- CSP implementations
- renderer-independent draw-command conversion
- WebGL command rendering

## Raw feature conversion

Upstream parsers should provide `RawEncFeature` values. Phase 1 converts them with:

```kotlin
val result = RawEncFeatureConverter.convert(raw)
```

Strict callers can use:

```kotlin
val feature = raw.toTypedFeature()
```

The converter checks:

- object-class acronym
- optional object-class code
- code/acronym mismatch
- object-class support for the supplied primitive type
- attribute acronym validity
- coarse value kind compatibility

## Why some numeric codes are nullable

This phase intentionally avoids pretending that the hand-written starter catalogue is a complete official S-57 catalogue import. Known codes are included where already used by the Phase 0 scaffold. Other entries keep `code = null` until Phase 2 adds the external catalogue importer/generator.
