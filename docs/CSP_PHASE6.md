# Phase 6 — Complete CSP coverage for the generated fixture

Phase 6 expands the CSP layer from the Phase 5 critical batch to every `CS(...)`
reference in the generated synthetic Presentation Library fixture.

The project still does **not** bundle official IHO Presentation Library assets.
The Phase 6 implementation is therefore a complete coverage framework and
starter behavior set for the repository fixture, with conservative renderer-
independent instructions. A future official/local importer can replace the
synthetic fixture and the same `CspCoverageValidator` will fail CI until every
newly referenced procedure is implemented.

## Registry

`DefaultCspRegistry.phase6Complete()` registers all `CspId` entries.

The Phase 6 set contains the Phase 5 critical procedures:

- `DEPARE`
- `DEPCNT`
- `SOUNDG`
- `WRECKS`
- `OBSTRN`
- `LIGHTS`
- `TOPMAR`

and the additional fixture procedures:

- `ACHARE`
- `RESARE`
- `PRCARE`
- `TESARE`
- `FAIRWY`
- `DRGARE`
- `SBDARE`
- `M_QUAL`
- `DATCVR`

## New CSP behavior

- Restricted/caution areas emit a synthetic area pattern, dashed boundary, and
  optional text from typed attributes such as `OBJNAM`, `RESTRN`, and `CATREA`.
- `DRGARE` emits depth-area color, dredged-area pattern, dashed boundary, and
  optional dredged depth text.
- `SBDARE` emits a seabed pattern and optional `NATCON` text.
- `M_QUAL` emits quality-of-data patterns based on `CATZOC`.
- `DATCVR` emits data coverage boundaries and a no-data fill/pattern for
  `CATCOV=2`.

## Definition of done

- The generated synthetic pack has zero missing `CS(...)` references with the
  Phase 6 registry.
- Every `CspId` has a command-level golden transcript test.
- New CSPs use typed `S57Attribute` access only.
- The root CI target is now `phase6Check`.
