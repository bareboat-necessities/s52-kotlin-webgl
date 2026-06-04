package io.github.s52.core.instruction

/** Registry-independent references extracted from a sequence of S-52 instructions. */
data class InstructionReferences(
    val symbols: Set<String> = emptySet(),
    val lineStyles: Set<String> = emptySet(),
    val patterns: Set<String> = emptySet(),
    val colorTokens: Set<String> = emptySet(),
    val csps: Set<String> = emptySet()
) {
    operator fun plus(other: InstructionReferences): InstructionReferences = InstructionReferences(
        symbols = symbols + other.symbols,
        lineStyles = lineStyles + other.lineStyles,
        patterns = patterns + other.patterns,
        colorTokens = colorTokens + other.colorTokens,
        csps = csps + other.csps
    )
}

object InstructionReferenceCollector {
    fun collect(instructions: Iterable<S52Instruction>): InstructionReferences =
        instructions.fold(InstructionReferences()) { acc, instruction -> acc + collect(instruction) }

    fun collect(instruction: S52Instruction): InstructionReferences = when (instruction) {
        is S52Instruction.Symbol -> InstructionReferences(symbols = setOf(instruction.name))
        is S52Instruction.SimpleLine -> InstructionReferences(
            lineStyles = setOf(instruction.style),
            colorTokens = setOf(instruction.colorToken)
        )
        is S52Instruction.ComplexLine -> InstructionReferences(lineStyles = setOf(instruction.name))
        is S52Instruction.AreaColor -> InstructionReferences(colorTokens = setOf(instruction.colorToken))
        is S52Instruction.AreaPattern -> InstructionReferences(patterns = setOf(instruction.name))
        is S52Instruction.Text -> InstructionReferences()
        is S52Instruction.Conditional -> InstructionReferences(csps = setOf(instruction.cspName))
    }
}
