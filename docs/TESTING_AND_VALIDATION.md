# Testing and validation

The project uses a staged validation ladder. Each layer catches a different class of error.

## 1. Unit tests

Unit tests cover typed catalogue lookup, raw-to-typed conversion, instruction parsing, lookup matching, CSP behavior, command validation, public API wiring, caching, and batching.

## 2. Static completeness validation

`StaticCompletenessValidator` checks Presentation Library packs before rendering. It reports:

- parse failures
- missing symbols
- missing line styles
- missing patterns
- missing colors or palettes
- missing CSP implementations
- invalid lookup primitive/object-class combinations
- duplicate source asset names
- invalid RGB values
- incompatible attribute-filter value kinds

## 3. Golden command transcripts

Golden tests compare deterministic `S52DrawCommandTranscript` output. They are intentionally command-level, not screenshot-level.

Benefits:

- stable across GPUs and fonts
- easier to review in diffs
- directly validates portrayal decisions

## 4. S-64 / Chart-1-style fixtures

The `s52-tests` module includes a small external fixture format used by `S64CommandValidationRunner`. The included fixtures are synthetic. Downstream projects can add private official or standards-derived validation data without committing restricted assets to this repository.

## 5. Renderer smoke tests

The WebGL renderer is exercised by the demo and JS build. It renders commands; it does not prove S-52 semantic correctness. Semantic correctness belongs to command-level tests and validation fixtures.

