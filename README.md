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

## Phase 2 status

Phase 2 is complete in this increment:

- Added a Presentation Library source-pack model for generated/imported data
- Added `PresLibPackBuilder` to build runtime registries from source packs
- Added a generated-style synthetic Phase 2 Presentation Library pack
- Added deterministic JVM Kotlin source generator API
- Added static Presentation Library validation reports
- Added validation for missing symbols, line styles, patterns, colors, and palettes
- Updated the default/Phase 0 pack to flow through the Phase 2 source pipeline
- Added Phase 2 JVM tests for generation, runtime pack construction, and validation
- CI now runs `phase2Check`

The project still does **not** bundle official IHO Presentation Library source assets. The Phase 2 pack is synthetic and exists to prove the architecture.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase2Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library source model, builder, validation, generator, registries
s52-csp           CSP implementations; early examples/placeholders
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

## Next step

Begin Phase 3: harden the S-52 instruction parser with source-location diagnostics, command serialization, and stricter support for the full Presentation Library instruction grammar.
