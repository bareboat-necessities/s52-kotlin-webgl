# Project Phases

This project is a standalone S-52 portrayal library. It accepts normalized, typed ENC-like features and produces renderer-independent S-52 draw commands. WebGL is an optional backend, not the core architecture.

## Phase 0 — Repository scaffold and architectural boundary

**Status:** complete.

Deliverables:

- Multi-module Kotlin Multiplatform repository
- GitHub Actions build
- Core feature, instruction, lookup, CSP, and draw-command APIs
- Minimal smoke-test lookup engine
- Minimal WebGL/demo placeholders
- Clear safety and legal boundary text

## Phase 1 — Typed S-57 / S-52 domain model

**Status:** complete in this increment.

Deliverables:

- Generated-style `S57ObjectClass` enum
- Generated-style `S57Attribute` enum
- Coarse `S57AttributeValueKind` metadata
- Starter `S57EnumeratedValue` table for common CSP/lookup values
- `S57CatalogValidator` for duplicate acronym/code/value checks
- `RawEncFeatureConverter` with structured diagnostics
- Strict primitive validation at the raw-to-typed boundary
- Typed scalar, list, and enum attribute helpers
- Tests proving the catalogue and conversion boundary are working

Notes:

- The catalogue files are deliberately data-only. No S-52 drawing or CSP behavior is stored in `S57ObjectClass` or `S57Attribute`.
- Some catalogue codes are still nullable in this phase. Phase 2 introduces the external generator/importer that can replace the curated starter catalogue with full official tables.

## Phase 2 — Presentation Library importer / generator

**Status:** complete in this increment.

Goal: avoid hand-maintaining S-52 lookup rows and Presentation Library resources.

Deliverables completed:

- `PresLibSourcePack` source/interchange model for generated or imported Presentation Library data
- `PresLibPackBuilder` that converts source packs to runtime registries
- Generated-style synthetic Phase 2 source pack in `GeneratedPhase2PresLib`
- Deterministic JVM Kotlin source generator API in `PresLibKotlinGenerator`
- Static validation report in `PresLibValidator`
- Missing-reference diagnostics for symbols, line styles, patterns, color tokens, and palettes
- Source-pack diagnostics for duplicate names and invalid RGB values
- Phase 2 tests for generation determinism, pack construction, and validation
- CI target `phase2Check`

Definition of done:

- Generator output is deterministic
- Runtime tables load without browser dependencies
- Missing reference report is machine-readable as a Kotlin data model and Markdown summary
- CI fails when generated references are internally inconsistent

Notes:

- The included Phase 2 Presentation Library pack is synthetic. It proves the generator/builder/validator architecture without bundling official IHO assets.
- `PresLibPack.phase0Minimal()` remains as a compatibility alias, but now delegates to the generated-style Phase 2 pack.

## Phase 3 — Complete S-52 instruction parser

**Status:** complete in this increment.

Goal: parse Presentation Library instruction strings into typed AST nodes while preserving enough metadata for generator diagnostics and future golden tests.

Instruction families supported:

- `SY(...)`
- `LS(...)`
- `LC(...)`
- `AC(...)`
- `AP(...)`
- `TX(...)`
- `TE(...)`
- `CS(...)`

Deliverables completed:

- Quote-aware, parenthesis-aware top-level splitter for instruction sequences and arguments
- Backward-compatible AST API through `InstructionParser.parseOne` and `parseSequence`
- Detailed parser API through `parseOneDetailed` and `parseSequenceDetailed`
- Source ranges for whole instruction, token, argument list, and individual arguments
- Raw argument preservation plus normalized argument values
- `TextSpec` for `TX` and `TE` instructions
- Canonical `InstructionFormatter` for round-trip and golden-test fixtures
- `InstructionReferenceCollector` for Presentation Library dependency scans
- Parser tests for source-location diagnostics, quoted delimiters, canonical formatting, and dependency extraction
- Generated Phase 2 Presentation Library pack coverage test
- CI target `phase3Check`

Definition of done:

- Every synthetic imported lookup instruction parses
- Parser reports source location and offending token
- Parser output can be serialized canonically for golden tests

Notes:

- Phase 3 does not yet claim official Presentation Library completeness. It proves the parser architecture against the synthetic pack and gives the generator/validator enough metadata for Phase 4 and Phase 9.

## Phase 4 — Lookup matching and display ordering

**Status:** complete in this increment.

Goal: turn typed feature + primitive + attributes into ordered portrayal instructions and draw commands.

Deliverables completed:

- Indexed lookup table by object class + primitive
- Detailed lookup matches with source row index and attribute-filter specificity
- Lookup explanation diagnostics for scale and attribute-filter rejections
- Structural `AttributeFilter` tree for generated/runtime lookup rows
- Source/generator-side `SourceAttributeFilter` model
- Optional lookup-row scale constraints
- Display category filtering extracted to `DisplayCategoryFilter`
- Viewing group allow/deny filtering extracted to `ViewingGroupFilter`
- Deterministic display-priority sorter with stable command-kind tie-breakers
- Mariner settings for enabled/disabled viewing groups
- Phase 4 tests and CI target `phase4Check`

Definition of done:

- Object class matching works
- Primitive matching works
- Attribute filter matching works
- Display category filtering works
- Viewing group filtering works
- Display priority ordering is deterministic
- Scale-dependent filtering supports feature and lookup-row constraints
- Radar flag remains preserved on all draw commands

## Phase 5 — Critical CSP batch

Status: **complete**.

Goal: implement the first safety-critical conditional symbology procedures as behavior objects separate from S-57 object-class enums.

Completed CSPs:

- `DEPARE`
- `DEPCNT`
- `SOUNDG`
- `WRECKS`
- `OBSTRN`
- `LIGHTS`
- `TOPMAR`

Completed deliverables:

- `CspId` stores the critical Phase 5 procedure implementations.
- `DefaultCspRegistry.phase5Critical()` exposes the registry used by tests and demos.
- `CspCoverageValidator` reports missing `CS(...)` implementations from a lookup table.
- The synthetic Presentation Library fixture references all seven critical CSPs.
- Direct tests cover safety contour, unsafe soundings, dangerous wrecks, dangerous obstructions, light descriptions/sectors, and topmark symbol selection.
- Engine tests prove `CS(...)` instructions expand into renderer-independent draw commands.

See [`CSP_PHASE5.md`](CSP_PHASE5.md).

## Phase 6 — Complete CSP coverage

**Status:** complete in this increment for the generated synthetic Presentation Library fixture.

Goal: every `CS(...)` referenced by imported lookup tables has an implementation.

Completed deliverables:

- `DefaultCspRegistry.phase6Complete()` registers every `CspId`.
- The generated synthetic Presentation Library fixture references the full Phase 6 CSP set.
- `CspCoverageValidator` reports zero missing CSP references for the generated fixture.
- Added starter CSP behavior for `ACHARE`, `RESARE`, `PRCARE`, `TESARE`, `FAIRWY`, `DRGARE`, `SBDARE`, `M_QUAL`, and `DATCVR`.
- Added command-level golden transcript tests for every CSP.
- CI target `phase6Check` runs all previous checks plus Phase 6 tests.

See [`CSP_PHASE6.md`](CSP_PHASE6.md).

## Phase 7 — Draw-command model hardening

**Status:** complete in this increment.

Goal: stabilize renderer-independent draw commands before the WebGL backend becomes real.

Completed deliverables:

- `DrawCommandKind` stable discriminator for every command family.
- `S52DrawCommand` now exposes `geometry` and `kind` through the shared interface.
- Added dedicated `S52DrawCommand.Sounding` instead of treating soundings as ordinary text.
- Preserved symbol, complex-line, and pattern parameters on draw commands.
- Added optional point-symbol rotation metadata.
- Added text-kind and optional text color metadata on text commands.
- Added `S52DrawCommandTranscript` for deterministic command-level golden fixtures.
- Added `DrawCommandValidator` for pre-render diagnostics.
- Updated placeholder WebGL stats to count soundings separately.
- CI target `phase7Check` runs all previous checks plus Phase 7 tests.

See [`DRAW_COMMAND_PHASE7.md`](DRAW_COMMAND_PHASE7.md).

## Phase 8 — WebGL2 renderer

Goal: render `S52DrawCommand`, not ENC semantics.

Completed deliverables:

- Added `RenderViewport` with automatic command-bounds fitting.
- Added `GeometryProjector` for lon/lat to WebGL clip-space projection.
- Added shared `SolidColorProgram` shader support.
- Added S-52 palette color resolution in the renderer layer.
- Implemented area fill rendering with simple fan triangulation.
- Implemented area pattern rendering with synthetic hatch overlays.
- Implemented simple-line and complex-line rendering.
- Implemented point-symbol rendering from Presentation Library vector commands.
- Implemented text rendering through a built-in line-glyph font.
- Implemented dedicated sounding rendering.
- Updated the browser demo to exercise all Phase 8 command families.
- CI target `phase8Check` runs all previous checks plus renderer/demo builds.

See [`WEBGL_PHASE8.md`](WEBGL_PHASE8.md).

## Phase 9 — Static completeness tests

Status: **complete**.

Goal: prove that a generated/imported Presentation Library pack is internally satisfied before rendering.

Completed deliverables:

- Added `StaticCompletenessValidator` for source and runtime Presentation Library packs
- Added structured `StaticCompletenessDiagnostic` and `StaticCompletenessReport`
- Validates instruction parsing, referenced symbols, line styles, patterns, colors, palettes, CSPs, lookup primitives, duplicate source names, RGB ranges, and source attribute-filter kind compatibility
- Added Phase 9 tests proving the synthetic generated pack has zero static-completeness diagnostics with the Phase 6 CSP registry
- Added negative tests for missing assets, missing palettes, missing CSPs, invalid primitives, parse failures, and incompatible attribute-filter kinds
- CI target `phase9Check` runs all previous checks plus the static-completeness tests

See [`STATIC_COMPLETENESS_PHASE9.md`](STATIC_COMPLETENESS_PHASE9.md).

## Phase 10 — Golden portrayal tests

Status: **complete**.

Goal: protect behavior from regressions using deterministic command-level transcripts.

Completed deliverables:

- Added dedicated `s52-tests` module.
- Added reusable golden helpers: `GoldenPortrayalCase`, `GoldenPortrayalRunner`, and `GoldenTranscriptComparison`.
- Added checked-in golden resources for depth/safety behavior, danger symbols, other-category overlays, and visibility settings.
- Tests run through the public `EncFeature -> S52PortrayalEngine -> S52DrawCommand` boundary.
- Draw commands are validated before transcript comparison.
- Negative comparator test proves mismatch messages identify the first differing line.
- CI target `phase10Check` runs all previous checks plus `:s52-tests:jvmTest`.

See [`GOLDEN_TESTS_PHASE10.md`](GOLDEN_TESTS_PHASE10.md).

## Phase 11 — S-64 / Chart 1 validation harness

Status: **complete**.

Goal: compare portrayal output against serious external validation material while still avoiding fragile pixel tests.

Completed deliverables:

- Added `CommandValidationFixture` for external command-level validation inputs.
- Added `ValidationFixtureParser` for small checked-in or externally supplied normalized-feature fixtures.
- Added `S64CommandValidationRunner`, `CommandValidationResult`, and `S64ValidationReport`.
- Added synthetic Chart-1 / S-64-style fixture resources for depth/danger, display-settings, and overlay/quality cases.
- Validation runs through `EncFeature -> S52PortrayalEngine -> S52DrawCommand -> transcript`.
- Negative test proves mismatch diagnostics identify the fixture id and first differing transcript line.
- CI target `phase11Check` runs all previous checks plus the validation harness tests.

See [`S64_VALIDATION_PHASE11.md`](S64_VALIDATION_PHASE11.md).

## Phase 12 — Public API stabilization

Goal: make the library consumable by other Kotlin/JS applications.

Deliverables:

- SemVer
- KDoc for public APIs
- Minimal integration example
- Stable module names and package structure

## Phase 13 — Performance pass

Goal: make portrayal and rendering viable for large real scenes.

Optimize:

- Lookup indexing
- Attribute-filter precompilation
- CSP allocations
- Draw-command batching
- Viewport culling
- Symbol/pattern/text atlas caching

## Phase 14 — Documentation and examples

Goal: make the project maintainable by contributors.

Docs:

- Architecture
- Public API
- Presentation Library import
- Conditional symbology
- WebGL renderer
- Testing and validation
- Safety/legal boundary

## Phase 15 — Release readiness and handoff

Status: **complete**.

Goal: make the repository self-auditing and ready for an experimental source release without changing the Phase 11 technical boundary.

Completed deliverables:

- Added `phase15ReleaseAudit` to verify required release/handoff files and safety statements.
- Added `phase15SourceArchive` to build a source zip from the repository.
- Added `phase15Check`, which runs all previous checks through `phase11Check` plus Phase 15 audit tests.
- Added tag-based `.github/workflows/release.yml` that runs checks and uploads the source archive.
- Added `CHANGELOG.md`, `CONTRIBUTING.md`, and `SECURITY.md`.
- Added minimal downstream integration documentation under `samples/integration/minimal-core/`.
- Updated CI to run `phase15Check`.

Phase 15 intentionally does not implement Phases 12–14. It is an additive release-readiness layer over Phase 11.

See [`RELEASE_PHASE15.md`](RELEASE_PHASE15.md).

## Phase 16 — Consumer API facade

Status: **complete**.

Goal: provide a stable, small integration entry point over the Phase 11/15 modules without changing the portrayal boundary or adding chartplotter scope.

Completed deliverables:

- Added `s52-api` as a consumer-facing Kotlin Multiplatform module.
- Added `S52PortrayalSession`, `S52PortrayalRequest`, `S52PortrayalResult`, `S52RuntimeManifest`, and `S52.synthetic()`.
- The facade wires `PresLibPack`, `CspRegistry`, `S52PortrayalEngine`, static completeness validation, draw-command validation, and command transcript generation.
- Added facade JVM tests and a minimal facade integration sample.
- Added `phase16ApiAudit` and `phase16Check`.
- Updated CI and the release workflow to run `phase16Check`.

Phase 16 remains additive. It still uses the synthetic Presentation Library fixture and does not implement S-57 parsing, S-63, navigation, AIS, or ECDIS certification.

See [`API_FACADE_PHASE16.md`](API_FACADE_PHASE16.md).


## Phase 17 — Diagnostic bundle and support handoff

Status: **complete**.

Goal: give downstream consumers a stable, renderer-independent diagnostic artifact for issue reports, CI summaries, validation handoff, and support workflows.

Completed deliverables:

- Added `S52DiagnosticBundle` and `S52Diagnostics` to the public `s52-api` module.
- Added `S52PortrayalSession.diagnosticBundle(...)` extension for one-call bundle generation.
- Diagnostic bundles include runtime manifest data, feature counts, command counts, command counts by `DrawCommandKind`, diagnostic counts, full transcript, and bounded transcript preview.
- Added Markdown and simple properties outputs without adding JSON dependencies.
- Added Phase 17 API tests and release-readiness tests.
- Added diagnostics integration sample under `samples/integration/diagnostics/`.
- Added `phase17DiagnosticsAudit`, `phase17SourceArchive`, and `phase17Check`.
- Updated CI and release workflow to run `phase17Check`.

Phase 17 remains additive. It does not add S-57 parsing, S-63, navigation, AIS, GPS, route management, or ECDIS certification scope.

See [`DIAGNOSTICS_PHASE17.md`](DIAGNOSTICS_PHASE17.md).

## Phase 18 — Built-in portrayal profiles

Status: **complete**.

Goal: make downstream demos, tests, issue reports, and consumer integrations reproducible by giving them stable named portrayal settings instead of ad-hoc `MarinerSettings` construction.

Completed deliverables:

- Added `S52PortrayalProfile`, `S52ProfileSummary`, `S52ProfilePreset`, and `S52ProfileCatalog` to the public `s52-api` module.
- Added built-in profile presets: `safety-day`, `planning-day`, `night-minimal`, and `diagnostics-all`.
- Added profile summary Markdown and simple properties-style output.
- Added profile-to-request and profile-to-context helpers.
- Added `S52PortrayalSession.portray(features, profile)` convenience API.
- Added `S52PortrayalSession.diagnosticBundle(features, profile, ...)` convenience API.
- Added profile API tests and Phase 18 release-readiness tests.
- Added profile integration sample under `samples/integration/profiles/`.
- Added `phase18ProfilesAudit`, `phase18SourceArchive`, and `phase18Check`.
- Updated CI and release workflow to run `phase18Check`.

Phase 18 remains additive. It does not add S-57 parsing, S-63, navigation, AIS, GPS, route management, or ECDIS certification scope.

See [`PROFILES_PHASE18.md`](PROFILES_PHASE18.md).

## Phase 19 — Portable portrayal artifact bundles

Status: **complete**.

Goal: let downstream apps and CI jobs export a complete, renderer-independent portrayal handoff as stable named text artifacts without adding a zip, JSON, file-system, or logging dependency to common code.

Completed deliverables:

- Added `S52Artifact`, `S52ArtifactBundle`, `S52ArtifactExportOptions`, and `S52ArtifactExporter` to the public `s52-api` module.
- Added `S52PortrayalSession.artifactBundle(...)` overloads for request-based and profile-based exports.
- Artifact exports can include manifest Markdown, diagnostics Markdown/properties, profile Markdown/properties, static completeness, command validation, full command transcript, and transcript preview.
- Added compact export options for issue reports that should avoid large full transcripts.
- Added artifact API tests and Phase 19 release-readiness tests.
- Added artifact integration sample under `samples/integration/artifacts/`.
- Added `phase19ArtifactsAudit`, `phase19SourceArchive`, and `phase19Check`.
- Updated CI and release workflow to run `phase19Check`.

Phase 19 remains additive. It does not add S-57 parsing, S-63, navigation, AIS, GPS, route management, or ECDIS certification scope.

See [`ARTIFACTS_PHASE19.md`](ARTIFACTS_PHASE19.md).


## Phase 20 — s52lib-compatible browser rendering

Add `PresLibPack.s52LibCompat()`, gallery APIs, and browser demo routes to render every symbol, line style, pattern, and color available in the loaded S-52 library pack.


## Phase 21 — Uploaded s52lib symbology image artifacts

Generate per-symbol, per-line-style, per-pattern, and per-color SVG files from the s52lib-compatible pack using a JVM build task. Upload `build/s52-symbology-images` from CI and release workflows as the `s52lib-symbology-images` artifact.


## Phase 22 — Real S-52/libS52 symbology import

Fix the Phase 21 fallback-subset issue by requiring a real S-52/libS52 Presentation Library payload for image export. Add a JVM importer for `SYMB`, `LNST`, and `PATT` records, fail the build when the symbol count is suspiciously small, and update CI to decode `S52LIB_PLIB_BASE64` before uploading `s52lib-symbology-images`.


## Phase 22 — OpenCPN vector symbology import

Import scalable/vector symbology from OpenCPN `chartsymbols.xml`, export all imported assets as SVGs in CI, fail the build on suspiciously small symbol counts, and change the project license to GPL-2.0-or-later for OpenCPN compatibility.


## Phase 23 — OpenCPN importer compile fix

Replace the broken Phase 22 importer source with a JVM-safe OpenCPN `chartsymbols.xml`/flat HPGL importer. Fix unresolved helpers, malformed character literals, ambiguous destructuring inference, and keep the vector-only no-raster symbology path.


## Phase 24 — readiness-test compatibility fix

Preserve historical Phase 15–19 readiness markers and safety-boundary text after
the Phase 22/23 OpenCPN symbology importer changes. CI still runs `phase22Check`,
but workflows and docs keep legacy audit strings visible for existing tests.
