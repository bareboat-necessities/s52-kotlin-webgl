## Phase 21

- Added a JVM exporter that generates per-asset SVG images from `S52LibCompatPresLib`, not `PresLibPack.phase2Synthetic()`.
- Added Gradle tasks `phase21GenerateSymbologyImages`, `phase21SymbologyImagesAudit`, `phase21SourceArchive`, and `phase21Check`.
- CI and release workflows now upload `build/s52-symbology-images` as the `s52lib-symbology-images` artifact.

## Phase 20

- Added s52lib-compatible Presentation Library pack using the public 63-token libS52/S52raz color set.
- Added browser gallery routes for all loaded symbols, line styles, patterns, and colors.
- Added `S52GalleryBuilder`, `S52GalleryRequest`, `S52GallerySection`, and `S52PortrayalSession.gallery(...)`.
- Demo now defaults to `PresLibPack.s52LibCompat()`.

# Changelog

## 0.1.0-SNAPSHOT

### Added

- Phase 0 repository scaffold and Kotlin/JS module structure.
- Typed S-57 catalogue subset and raw-to-typed conversion.
- Presentation Library source model, synthetic generated-style pack, and validation.
- S-52 instruction parser with quote-aware parsing and diagnostics.
- Lookup matching, display filtering, viewing-group filtering, and display ordering.
- Critical and expanded CSP registry coverage using synthetic fixtures.
- Renderer-independent `S52DrawCommand` model and deterministic transcripts.
- WebGL2 command renderer.
- Static completeness validation.
- Golden portrayal regression harness.
- S-64 / Chart-1-style command validation harness.
- Phase 15 release-readiness checks, release workflow, and source archive task.
- Phase 16 `s52-api` consumer facade, runtime manifest, diagnostics result, and facade integration sample.
- Phase 17 diagnostic bundle, Markdown/properties diagnostics, transcript preview, and diagnostics integration sample.
- Phase 18 built-in portrayal profiles, profile summaries, profile-based facade convenience calls, and profile integration sample.
- Phase 19 portable artifact bundles, artifact exporter API, artifact integration sample, and Phase 19 checks.

### Safety

- Experimental. Not type-approved ECDIS. Not for navigation.
- Official IHO Presentation Library assets are not bundled.
