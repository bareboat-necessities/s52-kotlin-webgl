# S-52 diagnostics API

`S52DiagnosticBundle` is a dependency-free support/reporting container in `s52-api`.
It summarizes a portrayal result, the source manifest, command counts, transcript
preview, static completeness, validation status, and error state.

Use it when a downstream chart engine, browser demo, or CI run needs a stable text
report without depending on renderer internals.

Typical usage:

```kotlin
val bundle = session.diagnosticBundle(
    features = features,
    profile = S52ProfileCatalog.diagnosticsAll,
    name = "customer-case-001"
)
println(bundle.toMarkdown())
```

Diagnostics are experimental, not type-approved ECDIS output, and not for navigation.
