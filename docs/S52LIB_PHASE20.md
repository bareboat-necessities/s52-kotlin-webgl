# Phase 20 — s52lib-compatible browser rendering

Phase 20 adds a browser path for rendering every asset available in the loaded S-52 library pack.

## What changed

- Added `PresLibPack.s52LibCompat()`.
- Added `S52.s52LibCompat()`.
- Added `S52GalleryBuilder` and `S52PortrayalSession.gallery(...)`.
- Added browser demo routes: `#chart`, `#symbols`, `#lines`, `#patterns`, `#colors`, and `#all`.
- Added the public libS52/S52raz 63-color token set to the compatibility pack.

## Important boundary

The public sduclos/S52 GitHub tree contains the libS52 renderer/parser code and the fallback `S52raz-3.2.rle` color payload. It does not bundle the restricted official IHO Presentation Library artwork. Therefore Phase 20 renders everything that is present in the loaded pack. If a larger s52lib-compatible source pack is generated locally, the same gallery renders all of those symbols, lines, patterns, and colors without changing the WebGL renderer.

## Browser usage

```bash
gradle :demo:jsBrowserDevelopmentRun
```

Then use hash routes:

```text
#chart
#symbols
#lines
#patterns
#colors
#all
```
