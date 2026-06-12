# WebGL performance phase 4: polygon clipping and bounded stencil fills

This patch continues the renderer-side performance work after the stencil-fill
phase.

## What changed

- Projected polygons are clipped to the current clip-space viewport before
  fill, pattern tiling, hatch generation, and CPU fallback triangulation.
- Stencil fills and stencil-clipped pattern draws now use a per-polygon scissor
  rectangle, so WebGL clears and writes only the polygon's screen-space bounds
  instead of the whole canvas for every area.
- Simple and complex line geometry is clipped to the visible clip rectangle
  before vertex upload. This reduces GPU buffer size while panning/zooming.
- HPGL parsing for complex line styles and vector area patterns is cached inside
  the renderer. Previously it parsed the same OpenCPN HPGL strings every render.
- Projected polygon bounds no longer allocate a flattened `outer + holes` list.

## Why this helps

Large ENC areas frequently extend far outside the current viewport. Drawing or
pattern-tiling against their full projected bounding boxes burns CPU and can make
fill artifacts look like long horizontal or vertical bands. Clipping first keeps
polygon work local to the visible screen and keeps fallback hatch/pattern output
bounded.

The stencil scissor is especially important because stencil-buffer clears are
otherwise full-canvas operations repeated for many area commands.

## Boundaries

This patch does not change S-52 portrayal decisions or object-class mapping. It
only changes WebGL renderer geometry preparation and GPU state usage.
