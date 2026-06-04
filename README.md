# s52-kotlin-webgl

A Kotlin Multiplatform / Kotlin-JS project for an S-52 marine chart portrayal library with an optional WebGL2 rendering backend.

This repository is intentionally **not** a chartplotter. It does not parse S-57, decrypt S-63, manage routes, ingest GPS/NMEA, show AIS, or implement navigation alarms. Its responsibility is:

```text
Typed ENC-like feature model in
        ↓
S-52 lookup / instruction / CSP portrayal
        ↓
Renderer-independent draw commands out
        ↓
Optional WebGL2 renderer
```

## Safety

Experimental. Not type-approved ECDIS. Not for navigation.

## Phase 1 status

Phase 1 is complete in this increment:

- Expanded generated-style `S57ObjectClass` enum with common S-57/S-52 portrayal classes
- Expanded generated-style `S57Attribute` enum with coarse value-kind metadata
- Added `S57EnumeratedValue` for common enumerated values used by early CSPs
- Added `S57CatalogValidator` to detect duplicate acronyms/codes/enumerated values
- Added strict raw-to-typed feature conversion through `RawEncFeatureConverter`
- Added detailed conversion diagnostics for unknown object classes, unknown attributes, and unsupported primitives
- Added typed helpers for scalar, list, and enumerated attribute access
- Added Phase 1 JVM tests for catalog validation and raw feature conversion
- CI now runs `phase1Check`

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase1Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library runtime model and Phase 0 minimal registry
s52-csp           CSP implementations; Phase 0 has examples/placeholders
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Later phases add an importer/generator so developers can provide their own local standards package and generate runtime tables.

## Next step

Begin Phase 2: add the Presentation Library importer/generator so lookup rows, symbols, line styles, patterns, colors, and CSP references are generated rather than hand-maintained.
