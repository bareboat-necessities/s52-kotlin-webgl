# Phase 4 Lookup Matching and Display Ordering

Phase 4 turns the Phase 0/1/2/3 scaffolding into a deterministic lookup pipeline:

typed feature + primitive + attributes + display scale
        ↓
indexed Presentation Library lookup rows
        ↓
matched S-52 instructions / CSP calls
        ↓
filtered and ordered draw commands

The core contract remains unchanged: the engine accepts normalized features and emits renderer-independent `S52DrawCommand` values. WebGL is still only a backend.

## What was added

- `LookupTable` now indexes rows by `S57ObjectClass + PrimitiveType`.
- `LookupTable.matchDetailed(...)` returns `LookupMatch` metadata for diagnostics and tests.
- `LookupTable.explain(...)` reports candidate count plus scale/filter rejections.
- `AttributeFilter` is now a structural typed filter tree.
- `LookupRecord` now supports optional row-level scale constraints.
- `DisplayCategoryFilter` owns S-52 display-category visibility rules.
- `ViewingGroupFilter` owns mariner-controlled viewing-group inclusion/exclusion.
- `DisplayPrioritySorter` gives deterministic painter-order sorting.
- `MarinerSettings` now has optional viewing-group allow/deny controls.
- The Presentation Library source model now has generator-friendly `SourceAttributeFilter`.

## Attribute filters

Runtime lookup rows use `AttributeFilter`:

```kotlin
AttributeFilter.EqualsInt(S57Attribute.CATWRK, 1)
AttributeFilter.IntIn(S57Attribute.COLOUR, setOf(1, 3))
AttributeFilter.DecimalRange(S57Attribute.VALSOU, maxInclusive = 10.0)
AttributeFilter.TextEquals(S57Attribute.OBJNAM, "Main Light", ignoreCase = true)
AttributeFilter.All(listOf(...))
AttributeFilter.AnyOf(listOf(...))
AttributeFilter.Not(...)
```

Generated/imported Presentation Library source packs should use `SourceAttributeFilter`, which converts to the runtime filter tree through `toRuntime()`.

## Matching rules

A row is a candidate only when object class and primitive match. A candidate becomes a match when:

1. feature-level scale constraints pass,
2. row-level scale constraints pass,
3. the row attribute filter matches.

Matches are ranked by attribute-filter specificity, then source order. This preserves deterministic behavior when a generic row and a more specific row both match.

## Display filtering

Display-category filtering happens after instructions/CSPs produce draw commands:

- `DisplayBase` shows only display-base commands.
- `Standard` shows display-base + standard commands.
- `Other` shows display-base + standard + other commands.
- `MarinersStandard` shows display-base + standard + mariner-standard commands.

Viewing groups are then filtered by `MarinerSettings.enabledViewingGroups` and `disabledViewingGroups`.

## Ordering

`DisplayPrioritySorter` sorts by:

1. S-52 display priority,
2. command-kind tie-breaker: area fill, pattern, line, symbol, text,
3. viewing group,
4. feature id,
5. radar flag.

The command-kind order is only a deterministic tie-breaker for synthetic/incomplete lookup data. Real Presentation Library display priority is still the primary key.

## Phase 4 definition of done

- Object-class matching: complete for typed feature model.
- Primitive matching: complete for typed feature model.
- Attribute filter matching: structural filters implemented and tested.
- Display category filtering: implemented and tested.
- Viewing group filtering: implemented and tested.
- Display priority ordering: deterministic and tested.
- Scale-dependent filtering: feature and row-level checks implemented.
- Radar flag preservation: still carried on all draw commands.
