package io.github.s52.core.instruction

/** Parsed instruction plus diagnostic metadata from the original source string. */
data class ParsedInstruction(
    val index: Int,
    val kind: InstructionKind,
    val instruction: S52Instruction,
    val sourceRange: InstructionSourceRange,
    val tokenRange: InstructionSourceRange,
    val argumentRange: InstructionSourceRange,
    val arguments: List<InstructionArgument>,
    val source: String
) {
    val raw: String get() = sourceRange.slice(source)
    val token: String get() = tokenRange.slice(source)
    val normalized: String get() = InstructionFormatter.format(instruction)
}

data class InstructionSequence(
    val source: String,
    val instructions: List<ParsedInstruction>
) {
    fun ast(): List<S52Instruction> = instructions.map { it.instruction }
    fun normalized(): String = instructions.joinToString(";") { it.normalized }
}
