# Profile integration sample

Phase 18 adds built-in portrayal profiles for downstream apps that want stable settings without manually constructing `MarinerSettings` for every call.

```kotlin
import io.github.s52.api.S52
import io.github.s52.api.S52ProfileCatalog
import io.github.s52.api.portray

val session = S52.synthetic()
val profile = S52ProfileCatalog.safetyDay

val result = session.portray(
    features = features,
    profile = profile
)

println(result.transcript)
```

For support reports, combine profiles with the Phase 17 diagnostic bundle:

```kotlin
import io.github.s52.api.diagnosticBundle

val bundle = session.diagnosticBundle(
    features = features,
    profile = S52ProfileCatalog.diagnosticsAll,
    name = "customer-case-001"
)

println(bundle.toMarkdown())
```

The bundled profiles are developer presets for repeatability. They are not certified ECDIS settings and are not for navigation.
