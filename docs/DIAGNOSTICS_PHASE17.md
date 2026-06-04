# Phase 17 — Diagnostic bundle and support handoff

Phase 17 adds a small diagnostics layer on top of the Phase 16 public facade. It is intended for downstream chart engines, CI artifacts, bug reports, and validation handoff.

The diagnostic bundle does not add chartplotter scope. It does not parse S-57, decrypt S-63, read GPS, manage routes, or claim ECDIS certification.

## Public API

```kotlin
val session = S52.synthetic()
val request = S52PortrayalRequest(features = features)
val bundle = session.diagnosticBundle(request)

println(bundle.toMarkdown())
println(bundle.toProperties())
```

## Contents

`S52DiagnosticBundle` includes:

- runtime manifest summary
- feature count
- command count
- command count by `DrawCommandKind`
- static completeness diagnostic count
- draw-command validation diagnostic count
- deterministic command transcript
- bounded transcript preview for issue reports

## Why this is separate from rendering

The bundle is produced from the public portrayal facade:

```text
EncFeature -> S52PortrayalSession -> S52PortrayalResult -> S52DiagnosticBundle
```

It does not inspect WebGL state and does not know S-57 object semantics beyond the already-typed feature model.

## Outputs

`toMarkdown()` is meant for human-readable issue reports and CI summaries.

`toProperties()` is a dependency-free machine-readable summary suitable for simple build artifacts.

The full `transcript` remains available for command-level regression comparison.
