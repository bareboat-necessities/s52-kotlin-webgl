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

## Phase 17 status

Phase 17 is complete in this increment:

- Phase 16 `s52-api` remains the stable consumer-facing facade.
- Added `S52DiagnosticBundle`, `S52Diagnostics`, and `S52PortrayalSession.diagnosticBundle(...)`.
- Diagnostic bundles summarize manifests, command counts by kind, validation counts, and deterministic transcript previews.
- Added a diagnostics integration sample.
- CI now runs `phase17Check`.

The project still uses a synthetic Presentation Library pack. Official IHO Presentation Library source assets are not bundled.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase17Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

To build the release handoff archive:

```bash
gradle phase17SourceArchive
```

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library source model, builder, validation, generator, registries
s52-csp           CSP implementations and coverage validation
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
s52-api           consumer-facing facade around core, preslib, and CSP modules
s52-tests         golden portrayal tests and S-64/Chart-1-style validation harness
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

## Next step

Use the Phase 16 facade and Phase 17 diagnostic bundle as the stable downstream integration and support boundary, then continue with broader performance work and official Presentation Library import tooling when ready.
