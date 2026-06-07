package io.github.s52.render.webgl.internal

import io.github.s52.core.settings.S52Palette
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
 * The first render call starts image loading and returns null. Later frames use
 * the ready texture. This keeps [WebGlS52Renderer.render] synchronous and avoids
 * changing the public renderer API in Phase 30.
 */
internal class RasterAtlasCache(
    private val gl: WebGLRenderingContext,
    private val basePath: String = "s52/opencpn",
    private val onAtlasReady: (() -> Unit)? = null
) {

    fun textureFor(palette: S52Palette, requestedAtlasFileName: String?): RasterAtlasTexture? {
        val fileName = paletteAtlasFileName(palette, requestedAtlasFileName)
        val url = "$basePath/$fileName"
        val entry = atlases.getOrPut(url) { Entry(gl, url).also { it.start() } }
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

    private class Entry(private val gl: WebGLRenderingContext, private val url: String) {
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
                gl.asDynamic().texImage2D(
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
                    url = url
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
            image.src = url
        }
    }

    private companion object {
        private val atlases = mutableMapOf<String, Entry>()
    }
}
