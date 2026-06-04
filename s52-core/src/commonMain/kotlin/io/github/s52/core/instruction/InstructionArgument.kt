package io.github.s52.core.instruction

/** One argument in an S-52 instruction, with both normalized and raw text retained. */
data class InstructionArgument(
    val value: String,
    val raw: String,
    val sourceRange: InstructionSourceRange,
    val wasQuoted: Boolean = false
)
