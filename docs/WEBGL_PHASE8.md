# Phase 8 — WebGL2 renderer

Phase 8 replaces the placeholder renderer with a command-only WebGL2 backend.
The renderer consumes `S52DrawCommand` values and deliberately does not inspect
S-57 object classes, S-57 attributes, lookup rows, or CSP names.

```text
S52DrawCommand list
        ↓
viewport projection
        ↓
command-family renderers
        ↓
shared WebGL2 shader program
        ↓
canvas
```

## Completed deliverables

- `RenderViewport` with automatic bounds from command geometry.
- `GeometryProjector` for equirectangular lon/lat to clip-space projection.
- Shared `SolidColorProgram` shader wrapper.
- S-52 palette color resolution through `ColorResolver`.
- Area fill rendering with simple fan triangulation.
- Area pattern rendering with synthetic hatch overlays.
- Simple and complex line rendering.
- Point-symbol rendering from Presentation Library vector commands.
- Text rendering through a small WebGL line-glyph font.
- Dedicated sounding rendering using the same line-glyph path.
- `RenderStats.drawCalls` for renderer smoke checks.
- Demo scene updated to exercise area fills, lines, symbols, text, soundings, and danger objects.

## Current limitations

This is the first real WebGL backend, not a certified ECDIS renderer.

- Polygon holes are ignored by the Phase 8 fan triangulator.
- Complex line styles are drawn as ordinary line strings for now.
- Area patterns are synthetic hatch overlays, not official S-52 pattern atlases.
- Text uses a compact built-in line-glyph font, not full SDF text.
- Symbol rendering draws Presentation Library vector paths as line segments.

These limitations are intentional for Phase 8. The architectural boundary is now
correct: portrayal produces renderer-independent commands, and WebGL renders only
those commands.

## Renderer boundary

Correct dependency direction:

```text
s52-core / s52-csp / s52-preslib  →  S52DrawCommand  →  s52-render-webgl
```

Incorrect dependency direction:

```text
s52-render-webgl  →  S57ObjectClass / S57Attribute / CSP logic
```

The WebGL renderer must remain ignorant of ENC semantics.
