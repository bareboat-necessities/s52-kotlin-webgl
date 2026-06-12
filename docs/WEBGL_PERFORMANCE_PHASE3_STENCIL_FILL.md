# WebGL performance phase 3: stencil polygon fills and clipped patterns

This patch moves the main area-fill path away from CPU trapezoid decomposition
when a stencil buffer is available.

## Why

The older fallback triangulator decomposes polygons into horizontal/vertical
bands.  It is deterministic and useful as a fallback, but complex S-57 polygons
can produce large numbers of bands.  Any numerical mistake in that decomposition
is also visible as a horizontal colour leak.

## What changed

* `WebGlS52Renderer` now requests a WebGL2 context with `stencil: true` and falls
  back to the previous context request if the browser ignores the option.
* `StencilPolygonClipper` implements even/odd GPU stencil filling for concave
  polygons and holes.
* `AreaFillRenderer` uses stencil fill first and keeps CPU triangulation only as
  a no-stencil fallback.
* The no-stencil fallback now splits batched fills by colour token.  This avoids
  rendering an adjacent run of different area colours with the first colour in
  the run.
* `AreaPatternRenderer` clips bitmap, vector, and hatch patterns through the same
  stencil mask when available.
* Pattern tile selection now tests tile/polygon rectangle intersection instead of
  only tile centre containment, reducing missing or leaking boundary tiles.
* `ProjectedPolygonClip` now has allocation-light point-in-polygon and rectangle
  intersection helpers instead of allocating triangulation objects for every
  containment test.
* `SolidColorProgram` and `TextureProgram` can upload directly from
  `FloatArrayBuilder`, avoiding a redundant `FloatArray` copy before creating the
  WebGL `Float32Array`.

## Fallback behavior

If the browser context has no stencil buffer, the renderer keeps using CPU
triangulation and CPU interval clipping.  This keeps compatibility with older or
restricted contexts, but best results are expected with the stencil path.
