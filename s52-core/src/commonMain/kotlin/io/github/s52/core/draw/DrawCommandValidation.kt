package io.github.s52.core.draw

/** Static checks that can run before commands reach a renderer. */
object DrawCommandValidator {
    fun validate(commands: List<S52DrawCommand>): DrawCommandValidationReport {
        val diagnostics = commands.flatMapIndexed { index, command -> validateOne(index, command) }
        return DrawCommandValidationReport(diagnostics)
    }

    private fun validateOne(index: Int, command: S52DrawCommand): List<DrawCommandDiagnostic> {
        val diagnostics = mutableListOf<DrawCommandDiagnostic>()

        if (command.priority < 0) {
            diagnostics += DrawCommandDiagnostic(index, command.featureId, "negative display priority: ${command.priority}")
        }
        if (command.viewingGroup < 0) {
            diagnostics += DrawCommandDiagnostic(index, command.featureId, "negative viewing group: ${command.viewingGroup}")
        }

        fun requireToken(value: String, label: String) {
            if (!isToken(value)) {
                diagnostics += DrawCommandDiagnostic(index, command.featureId, "$label is not a stable S-52 token: '$value'")
            }
        }

        when (command) {
            is S52DrawCommand.AreaFill -> requireToken(command.colorToken, "area fill color")
            is S52DrawCommand.AreaPattern -> {
                requireToken(command.patternName, "area pattern name")
                command.backgroundColorToken?.let { requireToken(it, "area pattern background color") }
            }
            is S52DrawCommand.LineSimple -> {
                requireToken(command.style, "simple line style")
                requireToken(command.colorToken, "simple line color")
                if (command.width <= 0.0) {
                    diagnostics += DrawCommandDiagnostic(index, command.featureId, "simple line width must be positive: ${command.width}")
                }
            }
            is S52DrawCommand.LineComplex -> requireToken(command.lineStyleName, "complex line style")
            is S52DrawCommand.PointSymbol -> requireToken(command.symbolName, "point symbol name")
            is S52DrawCommand.Text -> {
                if (command.textExpression.isBlank()) {
                    diagnostics += DrawCommandDiagnostic(index, command.featureId, "text expression is blank")
                }
                command.colorToken?.let { requireToken(it, "text color") }
            }
            is S52DrawCommand.Sounding -> {
                if (command.depthLabel.isBlank()) {
                    diagnostics += DrawCommandDiagnostic(index, command.featureId, "sounding depth label is blank")
                }
                requireToken(command.colorToken, "sounding color")
            }
        }

        return diagnostics
    }

    private fun isToken(value: String): Boolean =
        value.isNotBlank() && value.all { it.isUpperCase() || it.isDigit() || it == '_' }
}

data class DrawCommandValidationReport(
    val diagnostics: List<DrawCommandDiagnostic>
) {
    val hasErrors: Boolean get() = diagnostics.isNotEmpty()

    fun toMarkdown(): String = buildString {
        appendLine("# Draw Command Validation Report")
        appendLine()
        appendLine("Diagnostics: ${diagnostics.size}")
        diagnostics.forEach { diagnostic ->
            appendLine("- command[${diagnostic.commandIndex}] feature=${diagnostic.featureId}: ${diagnostic.message}")
        }
    }
}

data class DrawCommandDiagnostic(
    val commandIndex: Int,
    val featureId: Long,
    val message: String
)
