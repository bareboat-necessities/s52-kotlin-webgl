# Phase 5 — Critical CSP framework

Phase 5 turns conditional symbology procedures into first-class behavior objects. The project still uses a synthetic Presentation Library fixture, but the library boundary now mirrors the intended full S-52 architecture:

```text
LookupRecord instruction CS(NAME)
        ↓
CspRegistry
        ↓
ConditionalSymbologyProcedure.evaluate(...)
        ↓
S52Instruction list
        ↓
S52DrawCommand list
```

## Implemented critical CSPs

The default Phase 5 registry contains:

- `DEPARE` — depth area color selection from `DRVAL1`, `DRVAL2`, and mariner contour settings.
- `DEPCNT` — safety contour promotion using `VALDCO`.
- `SOUNDG` — sounding text generation, `showSoundings`, and unsafe/deep sounding color selection.
- `WRECKS` — dangerous/non-dangerous point and area instruction selection.
- `OBSTRN` — dangerous/non-dangerous point and area instruction selection.
- `LIGHTS` — symbol, optional sector line, and optional light description text.
- `TOPMAR` — synthetic topmark symbol selection from `TOPSHP`.

These are starter implementations for the critical behavior path. They intentionally use synthetic symbol, pattern, line-style, and color-token names supplied by the repository fixture. Official Presentation Library assets are not bundled.

## Registry coverage

`CspCoverageValidator` checks that every `CS(...)` reference in a lookup table is implemented by the selected registry. This gives a measurable path toward complete S-52 support: a later official/imported Presentation Library pack should fail CI until every referenced CSP has an implementation.

## Phase 5 definition of done

- Critical CSPs are implemented as separate objects.
- `CspId` lists the Phase 5 critical CSPs.
- `DefaultCspRegistry.phase5Critical()` registers them.
- The synthetic Presentation Library references all critical CSPs.
- Static CSP coverage has zero missing references.
- Tests cover depth contour, soundings, wrecks, obstructions, lights, topmarks, and engine expansion from `CS(...)` to draw commands.
