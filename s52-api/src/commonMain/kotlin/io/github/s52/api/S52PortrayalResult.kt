package io.github.s52.api

import io.github.s52.core.draw.DrawCommandValidationReport
import io.github.s52.core.draw.S52DrawCommand

/** Result returned by [S52Runtime.portrayValidated]. */
data class S52PortrayalResult(
    val commands: List<S52DrawCommand>,
    val validation: DrawCommandValidationReport
) {
    val isValid: Boolean get() = !validation.hasErrors
}
