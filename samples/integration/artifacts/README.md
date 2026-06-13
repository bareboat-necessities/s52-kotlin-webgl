# Artifact bundle integration sample

Adds a portable text-artifact export API for issue reports, CI handoff,
and downstream validation logs.

```kotlin
import io.github.s52.api.S52
import io.github.s52.api.S52ProfileCatalog
import io.github.s52.api.artifactBundle

val session = S52.synthetic()
val bundle = session.artifactBundle(
    features = features,
    profile = S52ProfileCatalog.safetyDay,
    name = "my-chart-engine-report"
)

for (artifact in bundle.artifacts) {
    println("${artifact.path}: ${artifact.mediaType}")
    println(artifact.preview(maxLines = 5))
}
```

Typical artifact paths:

```text
bundle-index.md
manifest.md
diagnostics.md
diagnostics.properties
profile.md
commands.jsonl
commands-preview.txt
```

The artifacts are text-only. The API does not write files or create zips by
itself, so it works in Kotlin common code. Platform code can save the files to
OPFS, IndexedDB, local disk, CI artifacts, or GitHub issue attachments.

Safety boundary: experimental, not type-approved ECDIS, not for navigation.
