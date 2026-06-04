# Phase 14 examples

Examples live in `samples/`. They are deliberately small and focus on integration boundaries rather than full chartplotter behavior.

## Minimal API

See `samples/minimal-api/MinimalApi.kt`.

The minimal path is:

```kotlin
val runtime = S52.defaultRuntime()
val settings = S52.defaultSettings()
val context = S52.defaultContext(settings)
val result = runtime.portrayValidated(features, settings, context)
```

The result contains renderer-independent commands and validation diagnostics.

## Deterministic transcript

See `samples/transcript/TranscriptExample.kt`.

Use transcripts for tests, debugging, and comparing portrayal behavior across versions:

```kotlin
val transcript = runtime.transcript(features, settings, context)
```

## Custom Presentation Library pack

See `samples/custom-preslib/CustomPresLibExample.kt`.

Use explicit runtime wiring when an application generates or loads its own Presentation Library pack:

```kotlin
val runtime = S52Runtime.from(
    presLib = myPresLibPack,
    cspRegistry = DefaultCspRegistry.phase6Complete()
)
```

## Browser/WebGL

See `samples/webgl-browser/README.md` and the `demo` module.

The browser renderer takes commands, a `PresLibPack`, and a viewport. It must not parse S-57 or evaluate CSPs.
