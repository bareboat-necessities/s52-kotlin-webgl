# Presentation Library Generator

Provides the generator API in JVM source:

```text
s52-preslib/src/jvmMain/kotlin/io/github/s52/preslib/generator/PresLibKotlinGenerator.kt
```

The generator accepts a normalized `PresLibSourcePack` and emits deterministic Kotlin source for a generated-style source-pack object.

This is an API-first generator. A command-line wrapper can be added later once the exact external source format is selected.

Official IHO Presentation Library assets are not bundled in this repository. Keep those as local/external inputs unless redistribution rights are clear.
