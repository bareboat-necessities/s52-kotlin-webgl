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

## Phase 5 status

Phase 5 is complete in this increment:

- Added a critical CSP registry through `CspId` and `DefaultCspRegistry.phase5Critical()`.
- Added starter implementations for `DEPARE`, `DEPCNT`, `SOUNDG`, `WRECKS`, `OBSTRN`, `LIGHTS`, and `TOPMAR`.
- Added static CSP coverage validation for `CS(...)` lookup references.
- Expanded the synthetic Presentation Library fixture so all critical Phase 5 CSPs are referenced.
- Added tests for direct CSP behavior and engine expansion from `CS(...)` to draw commands.
- CI now runs `phase5Check`.

The project still uses a synthetic Presentation Library pack. Official IHO Presentation Library source assets are not bundled.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase5Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library source model, builder, validation, generator, registries
s52-csp           CSP implementations and coverage validation
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

## Next step

Begin Phase 6: expand CSP coverage from the Phase 5 critical starter set toward every CSP referenced by an imported Presentation Library pack.
