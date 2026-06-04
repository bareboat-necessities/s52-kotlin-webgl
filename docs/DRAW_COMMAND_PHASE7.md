# Phase 7 — Draw Command Model Hardening

Phase 7 stabilizes the renderer-independent boundary between S-52 portrayal and any concrete renderer.

The important boundary remains:

```text
EncFeature + MarinerSettings
        ↓
S52PortrayalEngine
        ↓
List<S52DrawCommand>
        ↓
WebGL / golden tests / future renderers
```

## Completed in Phase 7

- Added `DrawCommandKind` as a stable discriminator for command families.
- Added `kind` and `geometry` to every `S52DrawCommand` through the shared interface.
- Added dedicated `S52DrawCommand.Sounding` so soundings are not hidden inside generic text commands.
- Preserved S-52 color/style/symbol references as tokens instead of CSS colors or renderer-specific assets.
- Preserved instruction parameters on symbol, complex-line, and area-pattern commands.
- Added optional symbol rotation metadata for future renderer support.
- Added `textKind` and optional text color token for text commands.
- Added `S52DrawCommandTranscript` for deterministic command-level golden tests.
- Added `DrawCommandValidator` for cheap pre-render static checks.
- Updated the placeholder WebGL renderer stats to count soundings separately.

## Why soundings are now separate

S-52 soundings behave like text visually, but they are semantically different:

- they are controlled by `showSoundings`, not just `showText`;
- they carry safety-depth coloring;
- they are commonly batched separately by renderers;
- golden tests need to distinguish ordinary text from sounding labels.

The engine therefore maps `TX(...)` or `TE(...)` emitted for `SOUNDG` features to `S52DrawCommand.Sounding`.

## Deterministic transcripts

`S52DrawCommandTranscript.serialize(commands)` emits stable JSON-lines-like text. It is intentionally not a public JSON contract. It is a simple, deterministic fixture format for tests and renderer debugging.

Example:

```text
{"kind":"area-fill","featureId":"1","priority":"1","viewingGroup":"21010","category":"Standard","overRadar":"false","geometry":"POLYGON(-74,40;-73.9,40;-73.9,40.1;-74,40)","color":"DEPVS"}
```

The transcript includes:

- command kind
- feature id
- display priority
- viewing group
- display category
- radar flag
- deterministic geometry summary
- command-specific tokens and parameters

## Validator

`DrawCommandValidator.validate(commands)` checks for mistakes that should never reach a renderer, such as:

- negative priority or viewing group;
- CSS-like color strings in places where S-52 color tokens are expected;
- blank text or sounding labels;
- non-positive simple-line widths.

This does not replace Presentation Library validation from Phase 9. It only validates the final command model.

## Definition of done

Phase 7 is complete when:

- all commands preserve feature id, geometry, priority, viewing group, category, and radar flag;
- command kinds are stable and renderer-independent;
- S-52 tokens remain as tokens until rendering;
- command transcripts are deterministic;
- soundings have a dedicated command type;
- command validation is covered by tests.
