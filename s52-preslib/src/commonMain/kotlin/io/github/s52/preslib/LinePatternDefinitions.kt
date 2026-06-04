package io.github.s52.preslib

data class LineStyleDefinition(
    val name: String,
    val description: String = ""
)

class LineStyleRegistry(
    private val styles: Map<String, LineStyleDefinition>
) {
    fun find(name: String): LineStyleDefinition? = styles[name.uppercase()]
    fun names(): Set<String> = styles.keys
    fun all(): List<LineStyleDefinition> = styles.values.sortedBy { it.name }
}

data class PatternDefinition(
    val name: String,
    val description: String = ""
)

class PatternRegistry(
    private val patterns: Map<String, PatternDefinition>
) {
    fun find(name: String): PatternDefinition? = patterns[name.uppercase()]
    fun names(): Set<String> = patterns.keys
    fun all(): List<PatternDefinition> = patterns.values.sortedBy { it.name }
}
