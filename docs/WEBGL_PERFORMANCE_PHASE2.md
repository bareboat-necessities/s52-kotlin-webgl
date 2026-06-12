# WebGL performance phase 2

This incremental patch targets browser-side renderer overhead that remains after
area-fill batching and raster atlas fixes.

## Changes

- Adds `FloatArrayBuilder`, a primitive-float vertex buffer used by hot renderers.
  This avoids `ArrayList<Float>` boxing while assembling WebGL vertices.
- Caches resolved palette colors inside each `ColorResolver` instance.  Repeated
  S-52 color tokens no longer re-run lookup/normalization for every command.
- Batches adjacent `LineSimple` commands when style, width, and color match.
  Batched line strips are uploaded as one `LINES` vertex list, avoiding many tiny
  GPU uploads/draw calls.
- Keeps the previous adjacent `AreaFill` batching and the Kotlin/JS WebGL2
  `unsafeCast` fix in `WebGlS52Renderer`.
- Removes `DrawCommandBatcher.report(commands)` from the WebGL render path.  That
  helper groups and sorts all commands just to produce stats; the renderer now
  reports cheap render-batch stats from the actual loop.

## Expected effect

The biggest wins are lower GC pressure and fewer WebGL uploads.  This should help
large NOAA cells where the command stream has thousands of area and line commands.

## Scope

This patch does not change S-52 portrayal logic or S-57 adaptation.  It only
changes renderer-side batching, vertex assembly, and diagnostics/statistics work.
