package io.github.s52.preslib

data class SymbolDefinition(
    val name: String,
    val pivotX: Double = 0.0,
    val pivotY: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val commands: List<VectorCommand> = emptyList()
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
}
