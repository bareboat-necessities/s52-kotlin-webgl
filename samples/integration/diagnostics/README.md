# Diagnostics integration sample

This sample shows the intended Phase 17 integration pattern for downstream projects.

```kotlin
import io.github.s52.api.S52
import io.github.s52.api.S52PortrayalRequest
import io.github.s52.api.diagnosticBundle

val session = S52.synthetic()
val request = S52PortrayalRequest(features = features)
val diagnostics = session.diagnosticBundle(
    request = request,
    name = "my-chart-engine-s52-runtime",
    transcriptPreviewLineLimit = 20
)

println(diagnostics.toMarkdown())
```

Attach the Markdown output and the full command transcript when reporting portrayal bugs. Do not attach proprietary chart material or official Presentation Library source assets unless you have the right to share them.

Safety boundary: this project is experimental, not type-approved ECDIS, and not for navigation.
