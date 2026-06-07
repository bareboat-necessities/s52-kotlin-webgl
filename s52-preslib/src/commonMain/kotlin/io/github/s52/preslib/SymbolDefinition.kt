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
    private val sourceOrder: List<SymbolDefinition> = symbols.values.sortedBy { it.name },
    private val sourceNames: Set<String> = symbols.keys
) {
    fun find(name: String): SymbolDefinition? = symbols[lookupKey(name)]

    fun require(name: String): SymbolDefinition =
        find(name) ?: error("Missing S-52 symbol $name")

    /**
     * Returns source-record names, not only unique lookup keys.
     *
     * OpenCPN chartsymbols.xml contains duplicate symbol names.  A Set cannot
     * contain duplicate strings, so duplicate source records are exposed with a
     * stable suffix (for example FOO#2) while [find] and [require] strip that
     * suffix back to the real lookup key.  This preserves the OpenCPN inventory
     * count expected by tests and diagnostics without changing runtime lookup
     * behavior for normal S-52 symbol references.
     */
    fun names(): Set<String> = sourceNames

    /** Returns complete source-order symbol records, including duplicate names. */
    fun all(): List<SymbolDefinition> = sourceOrder

    companion object {
        fun fromDefinitions(definitions: List<SymbolDefinition>): SymbolRegistry {
            val byName = linkedMapOf<String, SymbolDefinition>()
            val seen = mutableMapOf<String, Int>()
            val sourceNames = linkedSetOf<String>()
            definitions.forEach { definition ->
                val key = definition.name.uppercase()
                if (key !in byName) byName[key] = definition
                val ordinal = (seen[key] ?: 0) + 1
                seen[key] = ordinal
                sourceNames += if (ordinal == 1) key else "$key#$ordinal"
            }
            return SymbolRegistry(symbols = byName, sourceOrder = definitions, sourceNames = sourceNames)
        }

        private fun lookupKey(name: String): String = name.substringBefore('#').uppercase()
    }
}
