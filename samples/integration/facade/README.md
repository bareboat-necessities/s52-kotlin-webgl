# Minimal facade integration sample

Add the public facade module to a Kotlin Multiplatform consumer:

```kotlin
dependencies {
    implementation(project(":s52-api"))
}
```

Then portray normalized ENC-like features:

```kotlin
import io.github.s52.api.S52
import io.github.s52.api.S52PortrayalRequest

val session = S52.synthetic()
val result = session.portray(
    S52PortrayalRequest(features = features)
)

check(!result.hasErrors) { result.diagnosticsMarkdown() }
renderer.render(result.commands)
```

For diagnostics or golden tests:

```kotlin
println(result.transcript)
println(session.manifest().toMarkdown())
```

The facade does not parse S-57 and does not include official IHO Presentation
Library assets. It is only a stable convenience layer around the Phase 11/15
portrayal modules.
