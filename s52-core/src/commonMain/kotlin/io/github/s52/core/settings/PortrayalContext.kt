package io.github.s52.core.settings

data class PortrayalContext(
    val compilationScale: Double,
    val displayScale: Double,
    val viewportId: String = "default"
)
