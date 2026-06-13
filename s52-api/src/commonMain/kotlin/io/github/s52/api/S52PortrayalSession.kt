package io.github.s52.api

import io.github.s52.core.csp.CspRegistry
import io.github.s52.core.draw.DrawCommandValidator
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.validation.StaticCompletenessReport
import io.github.s52.preslib.validation.StaticCompletenessValidator

/**
 * Consumer-facing facade.
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
            validation = commandValidation,
            staticCompleteness = completeness
        )
    }

    fun gallery(request: S52GalleryRequest = S52GalleryRequest()): S52GalleryResult =
        S52GalleryBuilder.build(presLib = presLib, request = request)

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
        fun synthetic(
            failOnStaticCompletenessErrors: Boolean = true
        ): S52PortrayalSession = S52PortrayalSession(
            presLib = PresLibPack.synthetic(),
            cspRegistry = DefaultCspRegistry.complete(),
            failOnStaticCompletenessErrors = failOnStaticCompletenessErrors
        )

        fun s52LibCompat(
            failOnStaticCompletenessErrors: Boolean = true
        ): S52PortrayalSession = S52PortrayalSession(
            presLib = PresLibPack.s52LibCompat(),
            cspRegistry = DefaultCspRegistry.complete(),
            failOnStaticCompletenessErrors = failOnStaticCompletenessErrors
        )

        fun openCpn(
            failOnStaticCompletenessErrors: Boolean = false
        ): S52PortrayalSession = S52PortrayalSession(
            presLib = PresLibPack.openCpn(),
            cspRegistry = DefaultCspRegistry.openCpn(),
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
