# ESRI enhanced SVG portrayal-input set

This directory is the intermediate handoff between raw ESRI nautical chart SVGs and the generated ESRI/OpenCPN portrayal layer.

- `symbols/`, `lines/`, and `patterns/` follow the OpenCPN generated symbology name contract.
- `objects/` follows the OpenCPN lookup object acronym contract for object-level review.
- Resolved files preserve ESRI geometry, recolor monochrome paths with OpenCPN/S-52 day color hints, and add deterministic identity/category overlays.
- `opencpn-comparison.csv` records the OpenCPN bitmap/color reference used to style each ESRI-derived SVG.
- Unresolved slots are visible review placeholders, not generic substitutes for production portrayal.

Counts: `1018` symbols, `57` lines, `30` patterns, `231` objects.
