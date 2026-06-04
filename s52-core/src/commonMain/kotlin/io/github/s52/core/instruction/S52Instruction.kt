package io.github.s52.core.instruction

sealed interface S52Instruction {
    data class Symbol(val name: String) : S52Instruction
    data class SimpleLine(val style: String, val width: Double, val colorToken: String) : S52Instruction
    data class ComplexLine(val name: String) : S52Instruction
    data class AreaColor(val colorToken: String) : S52Instruction
    data class AreaPattern(val name: String) : S52Instruction
    data class Text(val textExpression: String, val rawArgs: List<String>, val kind: InstructionKind) : S52Instruction
    data class Conditional(val cspName: String) : S52Instruction
}
