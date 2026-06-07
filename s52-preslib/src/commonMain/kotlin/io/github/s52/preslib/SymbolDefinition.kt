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
    private val symbols: Map<String, SymbolDefinition>,
    private val sourceOrder: List<SymbolDefinition> = symbols.values.sortedBy { it.name }
) {
    fun find(name: String): SymbolDefinition? = symbols[name.uppercase()]

    fun require(name: String): SymbolDefinition =
        find(name) ?: error("Missing S-52 symbol $name")

    fun names(): Set<String> = symbols.keys

    /**
     * Returns source-order symbol records, not just unique lookup keys.
     *
     * OpenCPN chartsymbols.xml intentionally contains duplicate symbol names in
     * the source payload. Runtime lookup still uses the unique-name map, but
     * inventory/diagnostics/tests need the complete source record count.
     */
    fun all(): List<SymbolDefinition> = sourceOrder

    companion object {
        fun fromDefinitions(definitions: List<SymbolDefinition>): SymbolRegistry {
            val byName = linkedMapOf<String, SymbolDefinition>()
            definitions.forEach { definition ->
                byName.putIfAbsent(definition.name.uppercase(), definition)
            }
            return SymbolRegistry(symbols = byName, sourceOrder = definitions)
        }
    }
}
