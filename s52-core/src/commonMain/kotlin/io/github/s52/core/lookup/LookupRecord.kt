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
    val overRadar: Boolean = false
)
