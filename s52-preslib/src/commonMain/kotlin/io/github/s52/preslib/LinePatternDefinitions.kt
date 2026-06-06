package io.github.s52.preslib

data class LineStyleDefinition(
    val name: String,
    val description: String = "",
    val pivotX: Double = 0.0,
    val pivotY: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val colorRefs: List<String> = emptyList(),
    val bitmap: RasterBitmapDefinition? = null,
    val vectorHpgl: String? = null
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
    val description: String = "",
    val pivotX: Double = 0.0,
    val pivotY: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val colorRefs: List<String> = emptyList(),
    val bitmap: RasterBitmapDefinition? = null,
    val vectorHpgl: String? = null
)

class PatternRegistry(
    private val patterns: Map<String, PatternDefinition>
) {
    fun find(name: String): PatternDefinition? = patterns[name.uppercase()]
    fun names(): Set<String> = patterns.keys
    fun all(): List<PatternDefinition> = patterns.values.sortedBy { it.name }
}
