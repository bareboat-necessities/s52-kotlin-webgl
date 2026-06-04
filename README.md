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

See [`docs/SAFETY_LEGAL.md`](docs/SAFETY_LEGAL.md) before using this project in any marine application.

## Phase 14 status

Phase 14 is complete in this increment:

- Added architecture, safety/legal, contributor, CSP, testing, and example documentation.
- Added sample code for minimal API usage, deterministic transcripts, custom Presentation Library wiring, and browser/WebGL integration.
- Added documentation/example smoke tests.
- Bumped public API version metadata to `0.14.0-SNAPSHOT`.
- CI now runs `phase14Check`.

The project still uses a synthetic Presentation Library pack by default. Official IHO Presentation Library source assets are not bundled.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase14Check
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

## Documentation index

Start here:

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — module boundaries and data flow.
- [`docs/PUBLIC_API_PHASE12.md`](docs/PUBLIC_API_PHASE12.md) — stable facade API.
- [`docs/EXAMPLES_PHASE14.md`](docs/EXAMPLES_PHASE14.md) — sample integrations.
- [`docs/ADDING_CSP.md`](docs/ADDING_CSP.md) — adding conditional symbology procedures.
- [`docs/TESTING_AND_VALIDATION.md`](docs/TESTING_AND_VALIDATION.md) — static, golden, and S-64-style validation.
- [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) — development workflow.
- [`docs/PHASES.md`](docs/PHASES.md) — full project roadmap.

## Examples

Examples are intentionally small and live under `samples/`:

```text
samples/minimal-api/       minimal public API usage
samples/transcript/        deterministic command transcript generation
samples/custom-preslib/    explicit runtime wiring with custom PresLibPack
samples/webgl-browser/     browser/WebGL integration notes
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.
