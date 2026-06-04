# s52lib-compatible gallery sample

```kotlin
val session = S52.s52LibCompat()
val gallery = session.gallery(S52GalleryRequest(section = S52GallerySection.All))
val renderer = WebGlS52Renderer(canvas, session.presLib)
renderer.render(gallery.commands, MarinerSettings())
```

The same command path renders every symbol, line style, pattern, and palette token available in the loaded `PresLibPack`.
