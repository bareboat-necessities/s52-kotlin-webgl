# Phase 12 — Public API stabilization

Phase 12 introduces a stable high-level integration layer for downstream Kotlin/JVM and Kotlin/JS applications.

The lower-level modules remain available, but most applications should now start from `s52-api`.

## New module

```text
s52-api
```

The module re-exports the core catalogue/model/portrayal dependencies and provides a small facade around the generated/supplied Presentation Library pack and CSP registry.

## Main entry points

```kotlin
import io.github.s52.api.S52

val runtime = S52.defaultRuntime()
val settings = S52.defaultSettings(
    safetyDepthMeters = 10.0,
    safetyContourMeters = 10.0,
    scale = 50_000.0
)
val context = S52.defaultContext(settings)

val commands = runtime.portray(features, settings, context)
```

The public boundary is still:

```text
List<EncFeature>
        ↓
S52Runtime.portray(...)
        ↓
List<S52DrawCommand>
```

No S-57 parser, S-63 decryptor, chart database, GPS, AIS, or navigation logic is part of this API.

## Default runtime

```kotlin
val runtime = S52.defaultRuntime()
```

This uses:

- `PresLibPack.phase2Synthetic()`
- `DefaultCspRegistry.phase6Complete()`
- `S52PortrayalEngine`

The default runtime is useful for tests, examples, and renderer bring-up. Production applications should build a runtime from their own generated Presentation Library pack:

```kotlin
val runtime = S52Runtime.from(
    presLib = generatedOrLoadedPresLib,
    cspRegistry = DefaultCspRegistry.phase6Complete()
)
```

Official IHO Presentation Library source assets are still not bundled.

## Validated portrayal

```kotlin
val result = runtime.portrayValidated(features, settings, context)

if (!result.isValid) {
    result.validationIssues.forEach(::println)
}
```

This runs `DrawCommandValidator` after portrayal and returns both commands and diagnostics.

## Deterministic transcript

```kotlin
val transcript = runtime.transcript(features, settings, context)
```

The transcript is intended for golden tests, integration debugging, and external validation harnesses.

## Lookup explanation

```kotlin
val explanation = runtime.explainLookup(feature, settings, context)
```

This exposes the lookup-table candidates, matches, and rejection reasons without requiring application code to know the internals of `LookupTable`.

## Version metadata

```kotlin
println(S52.version) // 0.12.0-SNAPSHOT
```

Phase 12 establishes the public facade, but the project remains pre-1.0 while the official Presentation Library import path and validation corpus mature.
