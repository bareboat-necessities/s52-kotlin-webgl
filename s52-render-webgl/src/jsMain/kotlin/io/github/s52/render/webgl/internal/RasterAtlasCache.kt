package io.github.s52.render.webgl.internal

import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.opencpn.generated.OpenCpnRasterAtlasData
import kotlinx.browser.document
import org.khronos.webgl.WebGLRenderingContext
import org.khronos.webgl.WebGLTexture
import org.w3c.dom.HTMLImageElement

internal data class RasterAtlasTexture(
    val texture: WebGLTexture,
    val width: Int,
    val height: Int,
    val url: String
)

/**
 * Lazy browser loader for the OpenCPN raster-symbol atlases.
 *
 * Raster PNGs are embedded into generated Kotlin as data URIs by
 * :s52-preslib:generateOpenCpnRasterAtlasData.  Client applications using the
 * WebGL renderer no longer need to copy rastersymbols-*.png into their own web
 * resource directory; the source PNGs are only build-time generator inputs.
 *
 * The first render call starts image loading and returns null. Later frames use
 * the ready texture. This keeps [WebGlS52Renderer.render] synchronous and avoids
 * changing the public renderer API in Phase 30.
 */
internal class RasterAtlasCache(
    private val gl: WebGLRenderingContext,
    private val onAtlasReady: (() -> Unit)? = null
) {

    fun textureFor(palette: S52Palette, requestedAtlasFileName: String?): RasterAtlasTexture? {
        val fileName = paletteAtlasFileName(palette, requestedAtlasFileName)
        val dataUri = OpenCpnRasterAtlasData.dataUriFor(fileName) ?: return null
        val cacheKey = "embedded-opencpn-raster-atlas:$fileName"
        val entry = atlases.getOrPut(cacheKey) { Entry(gl, cacheKey, dataUri).also { it.start() } }
        if (entry.ready == null && onAtlasReady != null) entry.addReadyCallback(onAtlasReady)
        return entry.ready
    }

    private fun paletteAtlasFileName(palette: S52Palette, requestedAtlasFileName: String?): String = when (palette) {
        S52Palette.Dusk -> "rastersymbols-dusk.png"
        S52Palette.Night -> "rastersymbols-dark.png"
        S52Palette.DayBright,
        S52Palette.DayBlackBack,
        S52Palette.DayWhiteBack -> requestedAtlasFileName?.takeIf { it.isNotBlank() } ?: "rastersymbols-day.png"
    }

    private class Entry(
        private val gl: WebGLRenderingContext,
        private val sourceDescription: String,
        private val dataUri: String
    ) {
        var ready: RasterAtlasTexture? = null
        private var started = false
        private var failed = false
        private val readyCallbacks = mutableListOf<() -> Unit>()

        fun addReadyCallback(callback: () -> Unit) {
            if (ready != null) {
                callback()
            } else if (!readyCallbacks.contains(callback)) {
                readyCallbacks += callback
            }
        }

        fun start() {
            if (started || failed) return
            started = true
            val texture = gl.createTexture()
            if (texture == null) {
                failed = true
                return
            }

            gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture)
            gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE)
            gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE)
            gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.LINEAR)
            gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.LINEAR)

            val image = document.createElement("img") as HTMLImageElement
            image.onload = { _ ->
                gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture)
                uploadTexImage2DFromImage(
                    gl,
                    WebGLRenderingContext.TEXTURE_2D,
                    0,
                    WebGLRenderingContext.RGBA,
                    WebGLRenderingContext.RGBA,
                    WebGLRenderingContext.UNSIGNED_BYTE,
                    image
                )
                ready = RasterAtlasTexture(
                    texture = texture,
                    width = image.naturalWidth,
                    height = image.naturalHeight,
                    url = sourceDescription
                )
                val callbacks = readyCallbacks.toList()
                readyCallbacks.clear()
                callbacks.forEach { it() }
                null
            }
            image.onerror = { _, _, _, _, _ ->
                failed = true
                null
            }
            image.src = dataUri
        }
    }

    private companion object {
        private val atlases = mutableMapOf<String, Entry>()

        @Suppress("UnsafeCastFromDynamic")
        private fun uploadTexImage2DFromImage(
            gl: WebGLRenderingContext,
            target: Int,
            level: Int,
            internalFormat: Int,
            format: Int,
            type: Int,
            image: HTMLImageElement
        ) {
            val upload: dynamic = js("(function(gl,target,level,internalFormat,format,type,image){gl.texImage2D(target,level,internalFormat,format,type,image);})")
            upload(gl, target, level, internalFormat, format, type, image)
        }
    }
}
