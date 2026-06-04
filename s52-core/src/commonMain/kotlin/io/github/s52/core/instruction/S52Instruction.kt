package io.github.s52.core.instruction

sealed interface S52Instruction {
    data class Symbol(val name: String, val parameters: List<String> = emptyList()) : S52Instruction
    data class SimpleLine(val style: String, val width: Double, val colorToken: String) : S52Instruction
    data class ComplexLine(val name: String, val parameters: List<String> = emptyList()) : S52Instruction
    data class AreaColor(val colorToken: String) : S52Instruction
    data class AreaPattern(val name: String, val parameters: List<String> = emptyList()) : S52Instruction
    data class Text(
        val textExpression: String,
        val rawArgs: List<String>,
        val kind: InstructionKind,
        val spec: TextSpec = TextSpec(textExpression, rawArgs, kind)
    ) : S52Instruction
    data class Conditional(val cspName: String, val parameters: List<String> = emptyList()) : S52Instruction
}
