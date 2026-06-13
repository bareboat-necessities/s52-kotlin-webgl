# WebGL batching and HPGL fill checks

The WebGL batching path keeps point symbols, vector symbol strokes/fills, and
area patterns on the renderer-owned WebGL2 path instead of adding DOM/canvas
fallback rendering.

The current regression checks expect:

- HPGL display lists compiled under `HpglDisplayList.kt`
- batched point-symbol rendering through `SymbolRenderer.renderBatch(...)`
- batched area-pattern rendering through `AreaPatternRenderer.renderBatch(...)`
- demo access to `#opencpn-regression`

The fixture route is a development/regression tool only. It is experimental,
not type-approved ECDIS output, and not for navigation.
