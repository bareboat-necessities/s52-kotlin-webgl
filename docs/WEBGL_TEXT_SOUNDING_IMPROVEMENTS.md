# WebGL text and sounding improvements

This incremental renderer update improves S-52 text labels and depth sounding labels without changing portrayal output commands.

## Changes

- Expanded the vector line font from a small partial alphabet to the full uppercase alphabet plus common punctuation.
- Unsupported label characters are now treated as spaces instead of being drawn as `X` placeholders.
- Depth soundings keep the nautical integer-plus-small-lowered-decimal style, but with better bounds and centering.
- Text and soundings now draw with a small contrast halo/shadow pass for readability over fills and patterns.
- Labels are decluttered in screen space per render frame so dense soundings and text do not overwrite each other.
- Line-string labels anchor near the length midpoint instead of the first vertex.
- Polygon labels anchor near the projected polygon centroid instead of the first outer-ring vertex.
- Labels completely outside the viewport are skipped before WebGL upload.

## Notes

The declutter is renderer-local and deterministic for the existing draw-command order. Earlier commands keep priority because they claim screen-space label bounds first. `RenderStats` still counts the source draw commands; skipped labels simply contribute zero WebGL draw calls.
