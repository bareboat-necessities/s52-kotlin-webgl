# Project phases

The goal is a complete S-52 portrayal library in Kotlin, with renderer-independent output and an optional WebGL2 backend.

## Phase 0 — Scaffold and architecture baseline

Status: complete in this repository.

Deliverables:

- Multi-module Gradle project
- GitHub Actions CI
- Typed feature model
- Typed object class and attribute enum subset
- S-52 instruction AST and parser smoke implementation
- Minimal lookup and portrayal engine
- CSP registry interfaces
- Presentation Library runtime model
- WebGL2 renderer placeholder
- Browser demo placeholder
- JVM smoke tests

Definition of done:

- `gradle phase0Check` is the canonical validation command.
- Core modules compile without browser dependencies.
- WebGL renderer does not know S-57 feature semantics.
- README contains safety and scope boundaries.

## Phase 1 — Generated S-57 catalogue

Replace the Phase 0 hand-written enum subset with generated tables.

Deliverables:

- Generator for `S57ObjectClass`
- Generator for `S57Attribute`
- Generator for enumerated attribute values
- Raw-to-typed feature conversion diagnostics
- Tests for acronym/code lookup

Definition of done:

- No hand-maintained S-57 object/attribute catalogue except generator inputs.
- Unknown object classes and attributes produce clear diagnostics.
- Typed features use enums, not strings.

## Phase 2 — Presentation Library importer/generator

Do not hand-maintain lookup rows and artwork metadata.

Deliverables:

- Importer for external Presentation Library source package
- Generated lookup tables
- Generated color tables
- Generated symbol, line, pattern, and text registries
- Deterministic generated output
- Missing reference report

Definition of done:

- Every lookup row imports.
- Every generated instruction string is preserved for parser validation.
- Official assets remain external unless redistribution rights are explicitly clear.

## Phase 3 — Complete instruction parser

Parse S-52 instruction strings into typed AST.

Instruction families:

- `SY(...)`
- `LS(...)`
- `LC(...)`
- `AC(...)`
- `AP(...)`
- `TX(...)`
- `TE(...)`
- `CS(...)`

Definition of done:

- Every imported lookup instruction parses.
- Parser diagnostics include source lookup row context.
- Unknown or malformed instructions fail tests, not runtime rendering.

## Phase 4 — Lookup matching and display ordering

Implement matching from object class, primitive, and attributes to instructions.

Deliverables:

- Object/primitive lookup indexing
- Attribute filter matcher
- Display category filter
- Viewing group filter
- Display priority sorter
- Scale filtering
- Over/under radar flag preservation

Definition of done:

- Lookup records produce stable, sorted, renderer-independent output.
- All ordering metadata is retained in `S52DrawCommand`.

## Phase 5 — Critical CSPs

Implement the first high-value conditional symbology procedures.

Initial CSPs:

- `DEPARE`
- `DEPCNT`
- `SOUNDG`
- `WRECKS`
- `OBSTRN`
- `LIGHTS`
- `TOPMAR`

Definition of done:

- Depth/safety portrayal works from typed attributes and mariner settings.
- Synthetic golden tests cover common shallow/deep/danger cases.

## Phase 6 — Complete CSP coverage

Implement every CSP referenced by the imported Presentation Library.

Definition of done:

- Static test: every `CS(name)` in lookup tables is present in `CspRegistry`.
- Every CSP has command-level tests.
- No CSP reads attributes by raw string.

## Phase 7 — Draw-command completeness

Make renderer-independent command output complete enough for all S-52 instructions.

Commands:

- Area fill
- Area pattern
- Simple line
- Complex line
- Point symbol
- Text
- Sounding

Definition of done:

- Commands serialize for golden tests.
- Commands use S-52 color tokens, not CSS colors.
- Commands preserve feature id, priority, viewing group, and display category.

## Phase 8 — WebGL2 renderer

Render `S52DrawCommand` only.

Deliverables:

- Area fill renderer
- Pattern atlas renderer
- Simple and complex line renderers
- Point symbol renderer
- SDF text renderer
- Sounding renderer
- Day/dusk/night palette switching

Definition of done:

- WebGL layer contains no S-57 object-class logic.
- Renderer can draw synthetic Chart-1-like scenes.

## Phase 9 — Static completeness tests

Automatically verify internal Presentation Library consistency.

Checks:

- Every instruction parses
- Every symbol exists
- Every line style exists
- Every pattern exists
- Every color token exists in required palettes
- Every CSP exists
- Every object class maps to `S57ObjectClass`
- Every attribute maps to `S57Attribute`

Definition of done:

- Missing references fail CI.
- CI publishes or prints a completeness report.

## Phase 10 — Golden portrayal tests

Compare command-level output for synthetic fixtures.

Fixture families:

- Depth areas and contours
- Soundings
- Wrecks and obstructions
- Lights and sectors
- Buoys, beacons, and topmarks
- Restricted areas
- Quality and coverage features

Definition of done:

- Stable JSON golden outputs.
- Settings changes produce expected output changes.

## Phase 11 — S-64 / external validation harness

Add a validation harness for external test data.

Definition of done:

- Can run command-level validation against external fixtures.
- Can optionally produce PNG screenshots.
- Pixel-level tests are optional and tolerance-based.

## Phase 12 — Public API stabilization

Prepare the library for downstream use.

Definition of done:

- KDoc on public APIs
- SemVer versioning
- Usage examples
- Minimal integration guide
- Clear binary/source compatibility policy

## Phase 13 — Performance pass

Optimize portrayal and rendering.

Targets:

- Lookup indexing
- Attribute filter precompilation
- CSP allocation reduction
- Viewport culling
- Symbol/pattern atlas caching
- Line/text batching

Definition of done:

- Benchmarks exist.
- Large synthetic scenes remain interactive.

## Phase 14 — Documentation and contributor guide

Complete maintainability docs.

Documents:

- Architecture
- Presentation Library import
- CSP implementation guide
- WebGL renderer design
- Testing and validation
- Safety / not-for-navigation policy

Definition of done:

- New contributor can add a CSP and tests without reverse-engineering the codebase.
