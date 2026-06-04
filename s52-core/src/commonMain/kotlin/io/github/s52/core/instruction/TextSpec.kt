package io.github.s52.core.instruction

/** Parsed representation of TX/TE text instructions. */
data class TextSpec(
    val expression: String,
    val arguments: List<String>,
    val kind: InstructionKind
) {
    init {
        require(kind == InstructionKind.TX || kind == InstructionKind.TE) {
            "TextSpec kind must be TX or TE"
        }
    }
}
