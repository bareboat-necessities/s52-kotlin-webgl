# Minimal API example

This sample shows the preferred public integration path:

```kotlin
val runtime = S52.defaultRuntime()
val result = runtime.portrayValidated(features)
```

`S52.defaultRuntime()` uses the synthetic pack checked into this repository. Real applications should provide their own generated `PresLibPack` when integrating official Presentation Library data.
