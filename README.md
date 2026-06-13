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

## Critical check status

`criticalCheck` is the current full-check baseline:

The default browser demo uses the s52lib-compatible pack. The critical export/import tooling uses the bundled OpenCPN-compatible clean-check payload for repeatable CI checks. Use the external override path below to test against a full upstream OpenCPN `chartsymbols.xml` file.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

Use the same clean-check guard that CI uses:

```bash
bash scripts/critical-clean-check.sh
```

For a direct Gradle run without the extra preflight and lock-mutation guard:

```bash
gradle --no-daemon criticalCheck
```

The CI workflow installs Gradle and Java 21, then runs the clean-check guard from a clean checkout.

To build the release handoff archive:

```bash
gradle sourceArchive
```

To build the generated symbology ZIP locally:

```bash
gradle --no-daemon criticalSymbologyImagesArchive
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


## Public API helpers

The `s52-api` module exposes the stable consumer facade used by demos, tests, and downstream integrations:

- `S52PortrayalSession` creates synthetic or generated-library portrayal sessions and returns renderer-independent draw commands.
- `S52ProfileCatalog` provides deterministic built-in profiles such as `safetyDay`, `planningDay`, `nightMinimal`, and `diagnosticsAll` so examples and issue reports use repeatable mariner/display settings.
- `S52DiagnosticBundle` packages manifests, command summaries, transcript previews, and error state for support/debug reports.
- `S52ArtifactBundle` is the portable text-only export container produced by `S52ArtifactExporter` and `artifactBundle(...)`; downstream apps can write its artifacts to CI uploads, browser storage, local disk, or GitHub issue attachments.

These helpers are for integration, regression testing, and diagnostics. They do not make the output type-approved and they do not add navigation functions.

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

OpenCPN-derived symbology import and bundled OpenCPN-compatible symbology inputs require the project’s GPL-2.0-or-later licensing boundary.

## Browser gallery

Run `gradle :demo:jsBrowserDevelopmentRun` and open `#symbols`, `#lines`, `#patterns`, `#colors`, or `#all` to render the loaded S-52 library assets in browser.

## Critical OpenCPN symbology import

Use the bundled OpenCPN-compatible `chartsymbols.xml` clean-check payload for repeatable CI. The clean-check path validates that the XML, raster atlases, and committed Kotlin/JS Yarn lock are present before export.

```bash
bash scripts/critical-clean-check.sh
```

To test against a full upstream OpenCPN `chartsymbols.xml`:

```bash
gradle --no-daemon criticalCheck -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

or:

```bash
export OPENCPN_CHARTSYMBOLS_XML_FILE=/path/to/chartsymbols.xml
gradle --no-daemon criticalCheck
```

The generated GitHub Actions artifact is `opencpn-symbology-images-directory`. The generated GitHub Release asset is a ZIP named like `s52-kotlin-webgl-<version>-critical.zip`. It contains `s52-symbology-images/` with:

- `index.html`
- `manifest.properties`
- per-symbol SVG files under `symbols/`
- per-line-style SVG files under `lines/`
- per-pattern SVG files under `patterns/`
- color swatch SVG files under `colors/`
- `symbol-atlas-day.png`
- `symbol-atlas-dusk.png`
- `symbol-atlas-dark.png`

License note: the OpenCPN-compatible symbology import path requires **GPL-2.0-or-later** compatibility.
