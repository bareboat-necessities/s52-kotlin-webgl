package io.github.s52.api

import io.github.s52.core.csp.CspRegistry
import io.github.s52.core.draw.DrawCommandValidationReport
import io.github.s52.core.draw.DrawCommandValidator
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.draw.S52DrawCommandTranscript
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.validation.StaticCompletenessReport
import io.github.s52.preslib.validation.StaticCompletenessValidator

/**
 * Phase 16 consumer-facing facade.
 *
 * The facade deliberately remains small: it wires a Presentation Library pack,
 * a CSP registry, the core portrayal engine, command validation, and stable
 * transcript generation into one object that downstream chart engines can use
 * without depending on test harness internals.
 */
class S52PortrayalSession(
    val presLib: PresLibPack,
    val cspRegistry: CspRegistry,
    private val failOnStaticCompletenessErrors: Boolean = true
) {
    val engine: S52PortrayalEngine = S52PortrayalEngine(presLib.lookupTable, cspRegistry)

    val staticCompletenessReport: StaticCompletenessReport by lazy {
        StaticCompletenessValidator.validatePack(
            pack = presLib,
            implementedCsps = cspRegistry.names()
        )
    }

    fun portray(request: S52PortrayalRequest): S52PortrayalResult {
        val completeness = staticCompletenessReport
        if (failOnStaticCompletenessErrors && completeness.hasErrors) {
            error("S-52 Presentation Library pack is not statically complete.\n${completeness.toMarkdown()}")
        }

        val commands = engine.portray(
            features = request.features,
            settings = request.settings,
            context = request.context
        )
        val commandValidation = DrawCommandValidator.validate(commands)

        return S52PortrayalResult(
            commands = commands,
            commandValidation = commandValidation,
            staticCompleteness = completeness,
            transcript = S52DrawCommandTranscript.serialize(commands)
        )
    }

    fun manifest(name: String = "s52-kotlin-webgl-runtime"): S52RuntimeManifest {
        val completeness = staticCompletenessReport
        return S52RuntimeManifest(
            name = name,
            lookupRecords = completeness.lookupRecordCount,
            symbols = completeness.symbolCount,
            lineStyles = completeness.lineStyleCount,
            patterns = completeness.patternCount,
            palettes = completeness.paletteCount,
            referencedCsps = completeness.referencedCsps.size,
            implementedCsps = completeness.implementedCsps.size,
            diagnostics = completeness.diagnostics.size
        )
    }

    companion object {
        fun syntheticPhase16(
            failOnStaticCompletenessErrors: Boolean = true
        ): S52PortrayalSession = S52PortrayalSession(
            presLib = PresLibPack.phase2Synthetic(),
            cspRegistry = DefaultCspRegistry.phase6Complete(),
            failOnStaticCompletenessErrors = failOnStaticCompletenessErrors
        )
    }
}

/** Single-call request object for the stable public facade. */
data class S52PortrayalRequest(
    val features: List<EncFeature>,
    val settings: MarinerSettings = MarinerSettings(),
    val context: PortrayalContext = PortrayalContext(
        compilationScale = 50_000.0,
        displayScale = settings.scale
    )
)

/** Stable result object suitable for renderers, golden tests, and diagnostics. */
data class S52PortrayalResult(
    val commands: List<S52DrawCommand>,
    val commandValidation: DrawCommandValidationReport,
    val staticCompleteness: StaticCompletenessReport,
    val transcript: String
) {
    val hasErrors: Boolean get() = commandValidation.hasErrors || staticCompleteness.hasErrors

    fun diagnosticsMarkdown(): String = buildString {
        appendLine(staticCompleteness.toMarkdown())
        appendLine()
        appendLine(commandValidation.toMarkdown())
    }
}

/** Machine-readable runtime summary for release audits and downstream apps. */
data class S52RuntimeManifest(
    val name: String,
    val lookupRecords: Int,
    val symbols: Int,
    val lineStyles: Int,
    val patterns: Int,
    val palettes: Int,
    val referencedCsps: Int,
    val implementedCsps: Int,
    val diagnostics: Int,
    val safetyStatus: String = "Experimental; not type-approved ECDIS; not for navigation"
) {
    val staticallyComplete: Boolean get() = diagnostics == 0 && referencedCsps == implementedCsps

    fun toMarkdown(): String = buildString {
        appendLine("# S-52 Runtime Manifest")
        appendLine()
        appendLine("- Name: $name")
        appendLine("- Lookup records: $lookupRecords")
        appendLine("- Symbols: $symbols")
        appendLine("- Line styles: $lineStyles")
        appendLine("- Patterns: $patterns")
        appendLine("- Palettes: $palettes")
        appendLine("- Referenced CSPs: $referencedCsps")
        appendLine("- Implemented CSPs: $implementedCsps")
        appendLine("- Diagnostics: $diagnostics")
        appendLine("- Statically complete: $staticallyComplete")
        appendLine("- Safety status: $safetyStatus")
    }
}

/** Convenience entry point for consumers that want the default bundled synthetic fixture. */
object S52 {
    fun synthetic(): S52PortrayalSession = S52PortrayalSession.syntheticPhase16()
}
