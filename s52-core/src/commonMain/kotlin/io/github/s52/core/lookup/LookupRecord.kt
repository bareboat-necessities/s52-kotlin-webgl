package io.github.s52.core.lookup

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.catalog.S57ObjectClassKey
import io.github.s52.catalog.toKey
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.settings.DisplayCategory

data class LookupRecord(
    val objectClass: S57ObjectClass? = null,
    val primitive: PrimitiveType,
    val attributeFilter: AttributeFilter = AttributeFilter.Any,
    val instructions: List<S52Instruction>,
    val displayCategory: DisplayCategory,
    val viewingGroup: Int,
    val displayPriority: Int,
    val overRadar: Boolean = false,
    val minimumDisplayScale: Double? = null,
    val maximumDisplayScale: Double? = null,
    val sourceTableName: String? = null,
    val sourceDisplayPriorityLabel: String? = null,
    val sourceRadarPriority: String? = null,
    val sourceIndex: Int = 0,
    val objectClassKey: S57ObjectClassKey = objectClass?.toKey()
        ?: error("LookupRecord requires either objectClass or objectClassKey")
) {
    val presentationTable: LookupPresentationTable = LookupPresentationTable.parse(sourceTableName)

    init {
        require(objectClass?.supports(primitive) != false) {
            "Object class ${objectClass?.acronym} does not support primitive $primitive"
        }
        require(viewingGroup >= 0) { "Viewing group must be non-negative: $viewingGroup" }
        require(displayPriority >= 0) { "Display priority must be non-negative: $displayPriority" }
        require(minimumDisplayScale == null || minimumDisplayScale > 0.0) { "minimumDisplayScale must be positive" }
        require(maximumDisplayScale == null || maximumDisplayScale > 0.0) { "maximumDisplayScale must be positive" }
        require(
            minimumDisplayScale == null || maximumDisplayScale == null || minimumDisplayScale <= maximumDisplayScale
        ) { "minimumDisplayScale must be <= maximumDisplayScale" }
    }
}
