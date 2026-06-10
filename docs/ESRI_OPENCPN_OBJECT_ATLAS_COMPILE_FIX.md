# ESRI OpenCPN object atlas compile fix

This incremental patch fixes the JVM compilation failure in
`EsriSymbologyImageExportMain.kt` caused by escaped quotes inside a Kotlin
string interpolation expression:

```kotlin
${html(slot.esriName ?: \"unresolved\")}
```

Inside `${...}` Kotlin expects a normal expression; the escaped quotes are parsed
as string text and corrupt the rest of the file.  The fix computes the fallback
name before building the HTML line:

```kotlin
val esriPreviewName = html(slot.esriName ?: "unresolved")
```

The patch also retains the ESRI atlas behavior requested for Chartplotter:

- OpenCPN generated symbols, line styles, and patterns remain the coverage oracle.
- OpenCPN lookup object acronyms are exported under `objects/*.svg`.
- The manifest reports `objects=` and `objectContract=opencpn-lookup-object-compatible`.
- JSON and CSV reports include object coverage so CI can check OpenCPN/ESRI parity.

Apply this zip over the previous baseline or over the failed ESRI atlas patch.
