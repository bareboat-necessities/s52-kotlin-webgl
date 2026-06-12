# WebGL renderer / OpenCPN generated preslib restore

This incremental patch fixes an accidental file overwrite where the WebGL renderer
implementation was copied into:

`s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnGeneratedPresLib.kt`

That file must contain only generated OpenCPN presentation-library data and must
remain independent of browser/WebGL classes.  If it imports `HTMLCanvasElement`,
`WebGLRenderingContext`, or `io.github.s52.render.webgl.internal.*`, it is the
wrong content and common/JVM metadata compilation will fail.

The merged WebGL renderer belongs only in:

`s52-render-webgl/src/jsMain/kotlin/io/github/s52/render/webgl/WebGlS52Renderer.kt`

This patch restores `OpenCpnGeneratedPresLib.kt` from the generated preslib data
source and places the merged performance/text renderer in the renderer module.
