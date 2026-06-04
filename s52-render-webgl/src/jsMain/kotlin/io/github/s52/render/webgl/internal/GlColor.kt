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
        val color = token?.let { presLib.colors.color(palette, it) }
            ?: presLib.colors.color(palette, fallback)
            ?: S52Color(fallback, 0, 0, 0)
        return GlColor(color.r / 255.0f, color.g / 255.0f, color.b / 255.0f, 1.0f)
    }
}
