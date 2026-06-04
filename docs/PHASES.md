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

Status: **complete**.

Goal: make the library consumable by other Kotlin/JVM and Kotlin/JS applications.

Completed deliverables:

- Added `s52-api` as the stable high-level integration module.
- Added `S52` convenience entry point.
- Added `S52Runtime` facade around `PresLibPack`, `CspRegistry`, and `S52PortrayalEngine`.
- Added `S52Version` semantic-version metadata.
- Added `S52PortrayalResult` for validated command generation.
- Added default settings/context helpers.
- Added deterministic transcript and lookup-explanation helpers.
- Updated the browser demo to use the public API facade.
- Added Phase 12 API smoke tests.
- CI target `phase12Check` runs all previous checks plus `:s52-api:jvmTest`.

See [`PUBLIC_API_PHASE12.md`](PUBLIC_API_PHASE12.md).

## Phase 13 — Performance pass

Status: **complete**.

Goal: make portrayal and rendering viable for repeated repaint loops and larger scenes.

Completed deliverables:

- Added stable content-based `PortrayalRequestKey`.
- Added common-source `PortrayalCache` with hit/miss/eviction stats.
- Added public `S52CachedRuntime` facade.
- Added `S52.defaultRuntime().cached(...)` and `S52.cachedRuntime(...)`.
- Added `DrawCommandBatcher`, `DrawBatchKey`, `DrawBatchReport`, and `PortrayalPerformanceReport`.
- Extended WebGL `RenderStats` with batch metrics.
- Added Phase 13 tests for cache behavior, stable request keys, batch grouping, and public performance reports.
- CI target `phase13Check` runs all previous checks plus Phase 13 API/core tests.

See [`PERFORMANCE_PHASE13.md`](PERFORMANCE_PHASE13.md).

## Phase 14 — Documentation and examples

Status: **complete**.

Goal: make the project maintainable by contributors and downstream integrators.

Completed deliverables:

- Added architecture documentation with strict module boundaries.
- Added safety/legal boundary documentation.
- Added contributor guide.
- Added CSP extension guide.
- Added testing and validation guide.
- Added example integration guide.
- Added sample code for minimal API usage, deterministic transcripts, custom Presentation Library wiring, and browser/WebGL integration.
- Added documentation/example smoke tests.
- Bumped public API version metadata to `0.14.0-SNAPSHOT`.
- CI target `phase14Check` runs all previous checks plus Phase 14 documentation/example checks.

See [`DOCUMENTATION_PHASE14.md`](DOCUMENTATION_PHASE14.md).

