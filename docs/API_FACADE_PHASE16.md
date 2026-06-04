# Phase 16 — Consumer API facade

Phase 16 adds a small consumer-facing `s52-api` module. It does not change the
core portrayal architecture and does not add chartplotter responsibilities.

## Goal

Downstream Kotlin/JS or Kotlin/JVM projects should be able to run the synthetic
runtime pack through one stable entry point:

```kotlin
val session = S52.synthetic()
val result = session.portray(S52PortrayalRequest(features = features))
```

The facade wires together:

```text
PresLibPack
CspRegistry
S52PortrayalEngine
StaticCompletenessValidator
DrawCommandValidator
S52DrawCommandTranscript
```

The public boundary remains:

```text
EncFeature -> S52PortrayalSession -> S52PortrayalResult
```

## Added API

- `S52PortrayalSession`
- `S52PortrayalRequest`
- `S52PortrayalResult`
- `S52RuntimeManifest`
- `S52.synthetic()` convenience entry point

## Runtime diagnostics

Every `S52PortrayalResult` includes:

- renderer-independent draw commands
- draw-command validation report
- static Presentation Library completeness report
- deterministic command transcript

This lets consumers display, test, or log the result without reaching into the
golden-test or validation-harness modules.

## Safety boundary

Phase 16 still uses the synthetic Presentation Library fixture. It is useful for
integration and architecture tests, but it is not a type-approved ECDIS runtime
and is not for navigation.
