# OpenCPN embedded raster atlases

The WebGL renderer no longer loads `s52/opencpn/rastersymbols-*.png` from the host application's web resources at runtime.

The PNG atlases remain in `s52/opencpn` as source inputs only.  During the Gradle build, `:s52-preslib:generateOpenCpnRasterAtlasData` reads:

- `rastersymbols-day.png`
- `rastersymbols-dusk.png`
- `rastersymbols-dark.png`

and regenerates `s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnRasterAtlasData.kt` with chunked base64 `data:image/png` URIs.

`RasterAtlasCache` consumes `OpenCpnRasterAtlasData` directly.  Applications embedding `s52-render-webgl` only need the compiled Kotlin/JS library output and no longer need to copy the OpenCPN PNG files beside their own `index.html`.

The checked-in JVM generator entry point is:

```text
io.github.s52.preslib.opencpn.generator.OpenCpnRasterAtlasDataGeneratorMain
```

The normal build path uses the Gradle generation task before Kotlin compilation so changes to any source PNG refresh the Kotlin data automatically.
