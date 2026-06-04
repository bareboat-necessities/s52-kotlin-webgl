# Phase 21 — Uploaded s52lib symbology images

Phase 21 adds a JVM build/export step that generates image artifacts for every
asset in the configured s52lib-compatible S-52 Presentation Library pack.

## Build tasks

```bash
gradle --no-daemon phase21GenerateSymbologyImages
gradle --no-daemon phase21Check
```

Generated files are written to:

```text
build/s52-symbology-images/
  index.html
  manifest.properties
  symbols/*.svg
  lines/*.svg
  patterns/*.svg
  colors/*.svg
```

## CI artifact

GitHub Actions now uploads the generated folder as:

```text
s52lib-symbology-images
```

This happens on both normal CI and tag-based release runs.

## Non-synthetic source boundary

The exporter calls:

```kotlin
S52LibCompatPresLib.sourcePack()
S52LibCompatPresLib.pack()
```

It refuses to run if the pack edition does not identify the `s52lib` compatible
pack. The generated `manifest.properties` includes:

```text
edition=phase20-s52lib-compat
synthetic=false
```

That means the image artifact path is not using `PresLibPack.phase2Synthetic()`.
When a fuller real s52lib/IHO-compatible pack is wired into
`S52LibCompatPresLib`, the same build step will upload every symbol, line style,
pattern, and color from that pack.
