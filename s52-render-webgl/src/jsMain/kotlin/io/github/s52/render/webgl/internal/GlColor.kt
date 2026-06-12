package io.github.s52.render.webgl.internal

import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.S52Color

internal data class GlColor(val r: Float, val g: Float, val b: Float, val a: Float = 1.0f)

internal class ColorResolver(
    private val presLib: PresLibPack,
    private val palette: S52Palette
) {
    private val cache = HashMap<String, GlColor>()
    private val colorCache = HashMap<String, S52Color?>()

    fun resolve(token: String?, fallback: String = "CHBLK"): GlColor {
        val key = "${token.orEmpty()}\u0000$fallback"
        cache[key]?.let { return it }

        val color = token?.let(::resolveOpenCpnToken)
            ?: color(fallback)
            ?: S52Color(fallback, 0, 0, 0)
        val resolved = GlColor(color.r / 255.0f, color.g / 255.0f, color.b / 255.0f, 1.0f)
        cache[key] = resolved
        return resolved
    }

    private fun resolveOpenCpnToken(token: String): S52Color? {
        color(token)?.let { return it }
        val normalized = token.trim().uppercase()
        // OpenCPN HPGL color references commonly prefix display-context letters
        // such as A/D/E/U before the S-52 color token: ACHMGD -> CHMGD,
        // ADEPSC -> DEPSC, ALANDF -> LANDF, UTRFCF -> TRFCF.
        if (normalized.length > 1) {
            color(normalized.drop(1))?.let { return it }
        }
        return null
    }

    private fun color(token: String): S52Color? {
        val key = token.trim().uppercase()
        if (colorCache.containsKey(key)) return colorCache[key]
        val resolved = presLib.colors.color(palette, key)
        colorCache[key] = resolved
        return resolved
    }
}
