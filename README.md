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

## Phase 11 status

Phase 11 is complete in this increment:

- Added an S-64 / Chart-1-style command validation harness in `s52-tests`.
- Added an external fixture parser for normalized feature fixtures and expected command transcripts.
- Added synthetic Chart-1 and S-64-style validation fixtures for depth/danger, display settings, and overlay/quality cases.
- Validation runs through the public `EncFeature -> S52PortrayalEngine -> S52DrawCommand` boundary and produces markdown reports.
- CI now runs `phase11Check`.

The project still uses a synthetic Presentation Library pack. Official IHO Presentation Library source assets are not bundled.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase11Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library source model, builder, validation, generator, registries
s52-csp           CSP implementations and coverage validation
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
s52-tests         golden portrayal tests and S-64/Chart-1-style validation harness
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

## Next step

Begin Phase 12: public API stabilization and integration examples.
