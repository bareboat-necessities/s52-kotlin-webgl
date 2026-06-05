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

## Phase 21 status

Phase 21 is complete in this increment:

- Phase 16 `s52-api` remains the stable consumer-facing facade.
- Phase 17 diagnostic bundles remain the support and CI handoff format.
- Phase 18 built-in portrayal profiles remain the reproducible settings layer.
- Added Phase 19 portable text artifact bundles through `S52ArtifactBundle`, `S52ArtifactExporter`, and `S52PortrayalSession.artifactBundle(...)`.
- Artifact bundles can include manifest, diagnostics, profile summary, static-completeness report, command-validation report, and command transcript files.
- Added an artifact-bundle integration sample.
- Added Phase 20 s52lib-compatible pack and browser gallery routes.
- Added Phase 21 JVM image export for the s52lib-compatible pack.
- CI now runs `phase21Check` and uploads `build/s52-symbology-images` as the `s52lib-symbology-images` artifact.

The default browser demo now uses the s52lib-compatible pack. Official IHO Presentation Library source assets are still not bundled; the compatibility pack renders all assets present in the loaded pack.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase21Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

To build the release handoff archive:

```bash
gradle phase21SourceArchive
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

## Next step

Use the Phase 16 facade, Phase 17 diagnostic bundle, Phase 18 profile presets, and Phase 19 artifact bundles as the stable downstream integration/support boundary, then continue with broader performance work and official Presentation Library import tooling when ready.


## Browser gallery

Run `gradle :demo:jsBrowserDevelopmentRun` and open `#symbols`, `#lines`, `#patterns`, `#colors`, or `#all` to render the loaded S-52 library assets in browser.


## Phase 22 OpenCPN symbology import

Use OpenCPN `chartsymbols.xml` as the real scalable symbology source. No raster atlas is used in this path.

```bash
gradle --no-daemon phase22Check -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

or:

```bash
export OPENCPN_CHARTSYMBOLS_XML_FILE=/path/to/chartsymbols.xml
gradle --no-daemon phase22Check
```

The generated GitHub Actions artifact is `opencpn-symbology-images`.

License note: Phase 22 changes this project to **GPL-2.0-or-later** for compatibility with the OpenCPN symbology import path.
