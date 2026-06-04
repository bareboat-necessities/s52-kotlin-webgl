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

## Phase 12 status

Phase 12 is complete in this increment:

- Added `s52-api` as the stable high-level integration module.
- Added `S52`, `S52Runtime`, `S52Version`, and `S52PortrayalResult`.
- Added default settings/context helpers and one-call portrayal convenience.
- Added validated portrayal, deterministic transcript generation, and lookup explanation helpers.
- Updated the browser demo to use the public API facade.
- Added Phase 12 API tests and public API documentation.
- CI now runs `phase12Check`.

The project still uses a synthetic Presentation Library pack by default. Official IHO Presentation Library source assets are not bundled.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase12Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library source model, builder, validation, generator, registries
s52-csp           CSP implementations and coverage validation
s52-api           stable public facade for downstream applications
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
s52-tests         golden portrayal tests and S-64/Chart-1-style validation harness
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

## Next step

Begin Phase 13: performance pass and renderer/portrayal benchmarking.
