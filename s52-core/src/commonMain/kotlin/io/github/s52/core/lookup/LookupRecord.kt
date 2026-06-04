package io.github.s52.core.lookup

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.settings.DisplayCategory

data class LookupRecord(
    val objectClass: S57ObjectClass,
    val primitive: PrimitiveType,
    val attributeFilter: AttributeFilter = AttributeFilter.Any,
    val instructions: List<S52Instruction>,
    val displayCategory: DisplayCategory,
    val viewingGroup: Int,
    val displayPriority: Int,
    val overRadar: Boolean = false,
    val minimumDisplayScale: Double? = null,
    val maximumDisplayScale: Double? = null,
    val sourceIndex: Int = 0
) {
    init {
        require(viewingGroup >= 0) { "Viewing group must be non-negative: $viewingGroup" }
        require(displayPriority >= 0) { "Display priority must be non-negative: $displayPriority" }
        require(minimumDisplayScale == null || minimumDisplayScale > 0.0) { "minimumDisplayScale must be positive" }
        require(maximumDisplayScale == null || maximumDisplayScale > 0.0) { "maximumDisplayScale must be positive" }
        require(
            minimumDisplayScale == null || maximumDisplayScale == null || minimumDisplayScale <= maximumDisplayScale
        ) { "minimumDisplayScale must be <= maximumDisplayScale" }
    }
}
