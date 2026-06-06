package io.github.s52.preslib

data class SymbolDefinition(
    val name: String,
    val pivotX: Double = 0.0,
    val pivotY: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val commands: List<VectorCommand> = emptyList(),
    val colorRefs: List<String> = emptyList(),
    val bitmap: RasterBitmapDefinition? = null,
    val vectorHpgl: String? = null
)

data class RasterBitmapDefinition(
    val atlasFileName: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val pivotX: Double = 0.0,
    val pivotY: Double = 0.0,
    val originX: Double = 0.0,
    val originY: Double = 0.0
)

sealed interface VectorCommand {
    data class MoveTo(val x: Double, val y: Double) : VectorCommand
    data class LineTo(val x: Double, val y: Double) : VectorCommand
    data object ClosePath : VectorCommand
}

class SymbolRegistry(
    private val symbols: Map<String, SymbolDefinition>
) {
    fun find(name: String): SymbolDefinition? = symbols[name.uppercase()]

    fun require(name: String): SymbolDefinition =
        find(name) ?: error("Missing S-52 symbol $name")

    fun names(): Set<String> = symbols.keys

    fun all(): List<SymbolDefinition> = symbols.values.sortedBy { it.name }
}
