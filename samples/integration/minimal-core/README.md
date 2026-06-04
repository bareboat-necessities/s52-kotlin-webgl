# Minimal core integration sample

This sample shows the intended downstream boundary for a Kotlin/JS chart engine or chartplotter that wants to use this repository only for S-52 portrayal.

The downstream application is expected to parse S-57/S-101/S-57-like data itself and normalize it into `EncFeature` values. This library then evaluates S-52 lookup rows, conditional symbology procedures, mariner settings, and display ordering.

```kotlin
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack

val presLib = PresLibPack.phase2Synthetic()
val engine = S52PortrayalEngine(
    lookupTable = presLib.lookupTable,
    cspRegistry = DefaultCspRegistry.phase6Complete()
)

val features: List<EncFeature> = loadFeaturesFromYourOwnParser()

val commands = engine.portray(
    features = features,
    settings = MarinerSettings(),
    context = PortrayalContext(scaleDenominator = 20_000)
)

// Your renderer can consume S52DrawCommand directly.
// The optional s52-render-webgl module is only one backend.
render(commands)
```

The sample uses `PresLibPack.phase2Synthetic()` because official Presentation Library assets are not bundled in this repository. A production integration should provide a generated `PresLibPack` from locally supplied standards-derived assets when redistribution rights are clear.

## Safety

This project is experimental and not for navigation. Do not use it as a certified ECDIS component.
