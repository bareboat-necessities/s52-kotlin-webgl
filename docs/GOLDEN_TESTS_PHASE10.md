# Phase 10 — Command-level golden portrayal tests

Phase 10 adds deterministic regression tests for the public portrayal boundary:

```text
EncFeature list
    ↓
S52PortrayalEngine
    ↓
S52DrawCommand list
    ↓
S52DrawCommandTranscript
    ↓
checked-in golden transcript
```

The tests deliberately compare renderer-independent draw-command transcripts, not screenshots. Pixel tests are postponed because browser fonts, antialiasing, device pixel ratio, and GPU differences can create noisy failures before the core portrayal behavior is stable.

## New module

```text
s52-tests/
  src/commonMain/kotlin/io/github/s52/tests/golden/
    GoldenPortrayalCase.kt
    GoldenPortrayalRunner.kt
    GoldenTranscriptComparison.kt
    Phase10GoldenCases.kt
  src/jvmTest/kotlin/io/github/s52/tests/golden/
    GoldenPortrayalPhase10Test.kt
  src/jvmTest/resources/golden/
    depth-safety.golden
    danger-symbols.golden
    other-overlays.golden
    visibility-settings.golden
```

## Covered fixtures

- `depth-safety`: DEPARE depth area coloring, DEPCNT safety contour styling, and SOUNDG unsafe sounding color.
- `danger-symbols`: dangerous WRECKS and OBSTRN point symbol selection.
- `other-overlays`: DATCVR no-data coverage, RESARE restricted-area pattern/text, and M_QUAL low-quality-data pattern/text.
- `visibility-settings`: `showText=false`, `showSoundings=false`, and `showLightDescriptions=false` suppress only the expected commands while preserving light graphics.

## Rules enforced

- Golden tests use the real synthetic Presentation Library pack.
- Golden tests use `DefaultCspRegistry.phase6Complete()`.
- Draw commands are validated with `DrawCommandValidator` before transcript comparison.
- The transcript comparator reports the first mismatching line and prints both expected and actual transcripts.
- Expected files are plain text and reviewable in Git diffs.

## Build target

```bash
gradle phase10Check
```

`phase10Check` depends on all previous phase checks and `:s52-tests:jvmTest`.

## Adding a new golden case

1. Add a new `GoldenPortrayalCase` in `Phase10GoldenCases`.
2. Add it to `Phase10GoldenCases.all()`.
3. Run the case locally to inspect the transcript.
4. Commit the transcript as `s52-tests/src/jvmTest/resources/golden/<case-id>.golden`.
5. Keep the case synthetic and command-focused until the external S-64 / Chart 1 validation harness is added in Phase 11.
