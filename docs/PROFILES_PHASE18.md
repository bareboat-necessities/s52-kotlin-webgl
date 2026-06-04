# Phase 18 — Built-in portrayal profiles

Phase 18 adds a small, dependency-free profile layer to the public `s52-api` module.

The profile layer is intended for downstream chart engines, demos, issue reports, and CI fixtures that need reproducible display settings without manually assembling `MarinerSettings` in every call site.

## Scope

Phase 18 is still portrayal-only. It does not add S-57 parsing, S-63 support, navigation state, AIS, GPS, route management, or ECDIS certification scope.

## New public API

```kotlin
val profile = S52ProfileCatalog.safetyDay
val result = S52.synthetic().portray(features, profile)
```

The central types are:

- `S52PortrayalProfile`
- `S52ProfileSummary`
- `S52ProfilePreset`
- `S52ProfileCatalog`

The profile contains:

- stable profile id
- display name
- description
- complete `MarinerSettings`

The profile can produce:

- a ready-to-use `S52PortrayalRequest`
- a default `PortrayalContext`
- a Markdown summary
- a simple properties-style summary

## Built-in profiles

The synthetic fixture now exposes four built-in profile presets:

| Profile id | Purpose |
|---|---|
| `safety-day` | Standard day palette with text and soundings enabled. |
| `planning-day` | Other-category day profile for overlay review. |
| `night-minimal` | Night palette with reduced clutter. |
| `diagnostics-all` | Verbose profile for command transcripts and issue reports. |

These profiles are not a substitute for official ECDIS configuration, are not for navigation, and are stable developer presets for repeatable test and integration behavior.

## Convenience extensions

Phase 18 adds two convenience extensions on `S52PortrayalSession`:

```kotlin
val result = session.portray(features, S52ProfileCatalog.safetyDay)

val bundle = session.diagnosticBundle(
    features = features,
    profile = S52ProfileCatalog.diagnosticsAll,
    name = "support-case"
)
```

The extensions still route through the same Phase 16/17 facade boundary:

```text
EncFeature -> S52PortrayalSession -> S52PortrayalEngine -> S52DrawCommand -> transcript/diagnostics
```

## Definition of done

Phase 18 is complete when:

- profile ids are stable and unique
- every profile validates its `MarinerSettings`
- profile request creation works through the public facade
- profile-based diagnostic bundles work
- profile docs and sample are present
- CI runs `phase18Check`
