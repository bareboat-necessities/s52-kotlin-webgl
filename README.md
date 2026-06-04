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

## Phase 0 status

Phase 0 is complete in this scaffold:

- Multi-module Gradle project
- GitHub Actions workflow
- Typed S-57 object and attribute enum subset
- Renderer-independent `EncFeature`, `S52Instruction`, and `S52DrawCommand` APIs
- Minimal S-52 instruction parser for smoke tests
- Minimal lookup-driven portrayal engine
- Empty CSP registry plus example CSP module
- Minimal Presentation Library registry model
- WebGL2 renderer placeholder
- Browser demo placeholder
- JVM smoke tests for core modules
- Full phase roadmap in `docs/PHASES.md`

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase0Check
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

Begin Phase 1: replace the hand-written catalogue subset with generated `S57ObjectClass`, `S57Attribute`, and enumerated attribute value tables.
