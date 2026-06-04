package io.github.s52.preslib

import io.github.s52.core.settings.S52Palette

data class S52Color(
    val token: String,
    val r: Int,
    val g: Int,
    val b: Int
) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) {
            "RGB values must be in 0..255 for $token"
        }
    }
}

class ColorTables(
    private val tables: Map<S52Palette, Map<String, S52Color>>
) {
    fun color(palette: S52Palette, token: String): S52Color? = tables[palette]?.get(token.uppercase())

    fun requireColor(palette: S52Palette, token: String): S52Color =
        color(palette, token) ?: error("Missing S-52 color token $token in palette $palette")

    fun tokens(palette: S52Palette): Set<String> = tables[palette]?.keys.orEmpty()

    fun all(palette: S52Palette): List<S52Color> = tables[palette].orEmpty().values.sortedBy { it.token }
}
