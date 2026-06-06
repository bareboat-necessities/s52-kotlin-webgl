package io.github.s52.render.webgl.internal

import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.S52Color

internal data class GlColor(val r: Float, val g: Float, val b: Float, val a: Float = 1.0f)

internal class ColorResolver(
    private val presLib: PresLibPack,
    private val palette: S52Palette
) {
    fun resolve(token: String?, fallback: String = "CHBLK"): GlColor {
        val color = token?.let(::resolveOpenCpnToken)
            ?: presLib.colors.color(palette, fallback)
            ?: S52Color(fallback, 0, 0, 0)
        return GlColor(color.r / 255.0f, color.g / 255.0f, color.b / 255.0f, 1.0f)
    }

    private fun resolveOpenCpnToken(token: String): S52Color? {
        presLib.colors.color(palette, token)?.let { return it }
        val normalized = token.trim().uppercase()
        // OpenCPN HPGL color references commonly prefix display-context letters
        // such as A/D/E/U before the S-52 color token: ACHMGD -> CHMGD,
        // ADEPSC -> DEPSC, ALANDF -> LANDF, UTRFCF -> TRFCF.
        if (normalized.length > 1) {
            presLib.colors.color(palette, normalized.drop(1))?.let { return it }
        }
        return null
    }
}
