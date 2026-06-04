package io.github.s52.csp

import io.github.s52.core.csp.CspRegistry
import io.github.s52.core.instruction.InstructionReferenceCollector
import io.github.s52.core.lookup.LookupTable

/** Static coverage report proving that all CS(...) lookup references resolve to registered CSPs. */
data class CspCoverageReport(
    val referenced: Set<String>,
    val implemented: Set<String>,
    val missing: Set<String>
) {
    val hasErrors: Boolean get() = missing.isNotEmpty()

    fun toMarkdown(): String = buildString {
        appendLine("# CSP coverage report")
        appendLine()
        appendLine("- Referenced CSPs: ${referenced.size}")
        appendLine("- Implemented CSPs: ${implemented.size}")
        appendLine("- Missing CSPs: ${missing.size}")
        if (missing.isNotEmpty()) {
            appendLine()
            appendLine("## Missing")
            missing.sorted().forEach { appendLine("- `$it`") }
        }
    }
}

object CspCoverageValidator {
    fun validate(lookupTable: LookupTable, registry: CspRegistry): CspCoverageReport {
        val referenced = InstructionReferenceCollector.collect(
            lookupTable.records().flatMap { it.instructions }
        ).csps.mapTo(mutableSetOf()) { it.uppercase() }
        val implemented = registry.names().mapTo(mutableSetOf()) { it.uppercase() }
        return CspCoverageReport(
            referenced = referenced,
            implemented = implemented,
            missing = referenced.filterNot { registry.has(it) }.toSet()
        )
    }
}
