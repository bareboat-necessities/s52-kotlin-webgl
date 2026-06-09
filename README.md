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

## Phase 22 status

Phase 22 is the current full-check baseline:

- Phase 16 `s52-api` remains the stable consumer-facing facade.
- Phase 17 diagnostic bundles remain the support and CI handoff format.
- Phase 18 built-in portrayal profiles remain the reproducible settings layer.
- Phase 19 portable text artifact bundles remain available through `S52ArtifactBundle`, `S52ArtifactExporter`, and `S52PortrayalSession.artifactBundle(...)`.
- Phase 20 s52lib-compatible pack and browser gallery routes remain available.
- Phase 21 JVM image export remains as a compatibility alias.
- Phase 22 imports bundled OpenCPN `chartsymbols.xml` plus the three bundled raster-symbol PNG atlases and generates `opencpn-symbology-images`.
- CI runs `gradle --no-daemon phase22Check` from a clean checkout without requiring an external `OPENCPN_CHARTSYMBOLS_XML_FILE`.
- Kotlin 2.5 readiness notes are tracked in `docs/KOTLIN_25_READINESS.md`.

The default browser demo uses the s52lib-compatible pack. Phase 22 export/import tooling uses the bundled OpenCPN symbology inputs for repeatable CI checks.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle --no-daemon phase22Check
```

The CI workflow installs Gradle and Java 21, then runs the same task from a clean checkout.

To build the release handoff archive:

```bash
gradle phase22SourceArchive
```

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library source model, builder, validation, generator, registries
s52-csp           CSP implementations and coverage validation
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
s52-api           consumer-facing facade, diagnostics, profiles, artifact bundles, and gallery helpers
s52-tests         golden portrayal tests and S-64/Chart-1-style validation harness
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

OpenCPN-derived symbology import and bundled OpenCPN symbology inputs require the project’s GPL-2.0-or-later licensing boundary.

## Next step

Use the Phase 16 facade, Phase 17 diagnostic bundle, Phase 18 profile presets, and Phase 19 artifact bundles as the stable downstream integration/support boundary, then continue with broader performance work and official Presentation Library import tooling when ready.

## Browser gallery

Run `gradle :demo:jsBrowserDevelopmentRun` and open `#symbols`, `#lines`, `#patterns`, `#colors`, or `#all` to render the loaded S-52 library assets in browser.

## Phase 22 OpenCPN symbology import

Use bundled OpenCPN `chartsymbols.xml` as the real scalable symbology source. The clean-check path validates that the XML, raster atlases, and committed Kotlin/JS Yarn lock are present before export.

```bash
gradle --no-daemon phase22Check
```

To test against a different OpenCPN `chartsymbols.xml`:

```bash
gradle --no-daemon phase22Check -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

or:

```bash
export OPENCPN_CHARTSYMBOLS_XML_FILE=/path/to/chartsymbols.xml
gradle --no-daemon phase22Check
```

The generated GitHub Actions artifact is `opencpn-symbology-images`. It contains:

- `index.html`
- `manifest.properties`
- per-symbol SVG files under `symbols/`
- per-line-style SVG files under `lines/`
- per-pattern SVG files under `patterns/`
- color swatch SVG files under `colors/`
- `symbol-atlas-day.png`
- `symbol-atlas-dusk.png`
- `symbol-atlas-dark.png`

License note: Phase 22 changes this project to **GPL-2.0-or-later** for compatibility with the OpenCPN symbology import path.

## Kotlin 2.5 readiness

Project-owned Kotlin sources should not use `data class ... private constructor(...)` patterns. Those keys are regular classes with explicit equality now, and the JVM test suite includes a guard for that migration-warning pattern. See `docs/KOTLIN_25_READINESS.md`.

## Historical phase compatibility

The latest full check is `phase22Check`, but the project intentionally keeps the older readiness markers visible for downstream CI and release-audit tests: `phase15Check`, `phase16Check`, `phase17Check`, `phase18Check`, and `phase19Check`.

The Phase 18 profile API remains available through `S52ProfileCatalog`, and the Phase 19 artifact API remains available through `S52ArtifactBundle`.

Safety boundary: Experimental. Not type-approved ECDIS. Not for navigation.
