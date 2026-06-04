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

Goal: avoid hand-maintaining S-52 lookup rows and Presentation Library resources.

Deliverables:

- External Presentation Library source loader
- Deterministic generated Kotlin/JSON tables
- Generated lookup rows
- Generated color tables
- Generated symbol, line-style, and pattern registries
- Generated CSP reference report
- Build-time report for missing symbols, colors, line styles, patterns, and CSPs

Definition of done:

- Generator output is deterministic
- Runtime tables load without browser dependencies
- Missing reference report is machine-readable
- CI fails when generated references are internally inconsistent

## Phase 3 — Complete S-52 instruction parser

Goal: parse Presentation Library instruction strings into typed AST nodes.

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

- Every imported lookup instruction parses
- Parser reports source location and offending token
- Parser output can be serialized for golden tests

## Phase 4 — Lookup matching and display ordering

Goal: turn typed feature + primitive + attributes into ordered portrayal instructions.

Deliverables:

- Object class matching
- Primitive matching
- Attribute filter matching
- Display category filtering
- Viewing group filtering
- Display priority ordering
- Scale-dependent filtering
- Radar flag preservation

## Phase 5 — Critical CSP batch

Goal: implement the first safety-critical conditional symbology procedures.

Initial CSPs:

- `DEPARE`
- `DEPCNT`
- `SOUNDG`
- `WRECKS`
- `OBSTRN`
- `LIGHTS`
- `TOPMAR`

Definition of done:

- Critical depth and danger portrayal works
- CSP output remains renderer-independent
- Synthetic golden tests cover safety-depth/safety-contour settings

## Phase 6 — Complete CSP coverage

Goal: every `CS(...)` referenced by imported lookup tables has an implementation.

Definition of done:

- Zero missing CSP references
- Every CSP has command-level golden tests
- CSPs use typed attributes only

## Phase 7 — Draw-command model hardening

Goal: stabilize renderer-independent draw commands.

Definition of done:

- Commands preserve feature id, display priority, viewing group, category, and radar flag
- Commands use S-52 color tokens, not CSS colors
- Commands serialize deterministically for tests

## Phase 8 — WebGL2 renderer

Goal: render `S52DrawCommand`, not ENC semantics.

Deliverables:

- Area fill renderer
- Area pattern renderer
- Simple line renderer
- Complex line renderer
- Point symbol renderer
- Text renderer
- Sounding renderer
- Symbol, pattern, and SDF text atlases

## Phase 9 — Static completeness tests

Goal: prove the imported Presentation Library is internally satisfied.

Checks:

- Every instruction parses
- Every symbol reference resolves
- Every line-style reference resolves
- Every pattern reference resolves
- Every color token resolves in required palettes
- Every CSP reference resolves
- Every object/attribute acronym resolves to a typed enum

## Phase 10 — Golden portrayal tests

Goal: protect behavior from regressions.

Start with command-level tests for:

- DEPARE depth-color transitions
- DEPCNT safety contour
- SOUNDG safety depth
- WRECKS dangerous/non-dangerous cases
- OBSTRN dangerous/non-dangerous cases
- LIGHTS sector/description handling
- TOPMAR with buoy/beacon classes
- RESARE category combinations
- M_QUAL / CATZOC quality display
- DATCVR coverage boundaries

## Phase 11 — S-64 / Chart 1 validation harness

Goal: compare portrayal output against serious external validation material.

Start with command-level transcripts. Pixel-level regression can follow later because anti-aliasing, fonts, and GPU differences make screenshots fragile.

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
