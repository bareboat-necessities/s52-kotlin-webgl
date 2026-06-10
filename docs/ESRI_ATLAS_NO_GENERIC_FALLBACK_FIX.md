# ESRI atlas no-generic-fallback fix

This patch removes the generic category fallback behavior from the ESRI/OpenCPN
atlas exporter.

## Problem

The previous exporter tried to keep every OpenCPN slot visually filled by using
category fallback SVGs such as generic beacon, navigation-line, restricted-area,
or sand symbols whenever it could not find a real ESRI match.  That made the
atlas look complete, but it was wrong: many unrelated OpenCPN names all showed
the same fallback ESRI artwork.

## Fix

Unmatched OpenCPN symbol, line, pattern, and lookup-object slots are now exported
as `UNRESOLVED` blank SVG placeholders instead of generic fallback symbols.
The atlas PNG leaves those cells blank, the HTML report labels them as
`unresolved`, and the JSON/CSV/manifest reports expose unresolved counts.

The exporter now resolves slots using only:

1. explicit alias TSVs under `s52/esri/`,
2. exact ESRI SVG filename/base-name matches,
3. direct `CustomSymbolMap.xml` object-to-symbol conditions parsed with the
   real ESRI CustomSymbolMap parser, and
4. semantic token matches.

There is no `CATEGORY_FALLBACK` or `RENDER_FALLBACK` match kind anymore.

## Why this is safer

Missing mappings are now visible as missing mappings.  The artifact no longer
pretends that a generic ESRI symbol is the presentation for many unrelated
OpenCPN/S-52 objects.
