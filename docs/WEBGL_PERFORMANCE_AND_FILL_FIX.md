# WebGL performance, fill, and sounding rendering fix

This incremental patch focuses on browser-side renderer behavior reported by the
S-57 client application:

- slow rendering / browser jank;
- area and pattern fills that can look like horizontal color leakage;
- odd-looking depth sounding labels.

## Changes

### Faster WebGL uploads

`FloatArray.toFloat32Array()` no longer calls `toTypedArray()`.  The previous
path boxed every vertex component before every `bufferData()` upload.  The new
implementation copies directly into a JavaScript `Float32Array`.

### Batched solid area fills

Adjacent `AreaFill` commands with the same color token are now uploaded and drawn
as one WebGL call.  This preserves draw order while reducing WebGL state changes
and buffer uploads for the common case where the portrayal engine emits many
same-color area fills together.

### Safer projected polygon preprocessing

Area-fill rings are simplified after projection with a sub-pixel tolerance.  This
removes duplicate and nearly collinear points before triangulation, reducing CPU
cost and reducing skinny-triangle artifacts from dense ENC coast/depth polygons.

### Clipped fallback hatch pattern

The old fallback area-pattern renderer drew eight horizontal lines across the
feature bounding box.  When a pattern was missing or not ready, those lines could
extend outside the polygon and look like leaked color.  The fallback hatch now
intersects each scanline with the projected polygon and holes, so only in-area
segments are emitted.

### Raster atlas sampling

OpenCPN raster atlases now use nearest-neighbour filtering and inset UVs.  This
avoids sampling neighbouring atlas cells, which can show up as stray color
streaks around raster symbols and bitmap patterns.

### Sounding labels

Depth soundings now render decimal values in a hydrographic style: the integer
part is normal size and the first decimal digit is smaller and lowered, rather
than drawing a full-size decimal point string.
