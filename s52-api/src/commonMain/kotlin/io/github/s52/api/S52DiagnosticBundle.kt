package io.github.s52.api

import io.github.s52.core.draw.DrawCommandKind
import io.github.s52.core.draw.S52DrawCommand

/**
 * Diagnostic handoff bundle.
 *
 * The bundle is intentionally renderer-independent and dependency-light. It is
 * suitable for issue reports, CI artifacts, validation handoff, and downstream
 * chart-engine integration logs. It does not include raw chart files or any
 * official Presentation Library assets.
 */
data class S52DiagnosticBundle(
    val manifest: S52RuntimeManifest,
    val featureCount: Int,
    val commandCount: Int,
    val commandCountsByKind: Map<DrawCommandKind, Int>,
    val staticDiagnostics: Int,
    val commandDiagnostics: Int,
    val transcript: String,
    val transcriptPreviewLineLimit: Int = DEFAULT_TRANSCRIPT_PREVIEW_LINES
) {
    val hasErrors: Boolean get() = staticDiagnostics > 0 || commandDiagnostics > 0 || !manifest.staticallyComplete

    fun toMarkdown(): String = buildString {
        appendLine("# S-52 Diagnostic Bundle")
        appendLine()
        appendLine("## Runtime")
        appendLine()
        appendLine("- Name: ${manifest.name}")
        appendLine("- Safety status: ${manifest.safetyStatus}")
        appendLine("- Statically complete: ${manifest.staticallyComplete}")
        appendLine("- Lookup records: ${manifest.lookupRecords}")
        appendLine("- Symbols: ${manifest.symbols}")
        appendLine("- Line styles: ${manifest.lineStyles}")
        appendLine("- Patterns: ${manifest.patterns}")
        appendLine("- Palettes: ${manifest.palettes}")
        appendLine("- Referenced CSPs: ${manifest.referencedCsps}")
        appendLine("- Implemented CSPs: ${manifest.implementedCsps}")
        appendLine()
        appendLine("## Portrayal request/result")
        appendLine()
        appendLine("- Features: $featureCount")
        appendLine("- Commands: $commandCount")
        appendLine("- Static diagnostics: $staticDiagnostics")
        appendLine("- Command diagnostics: $commandDiagnostics")
        appendLine("- Has errors: $hasErrors")
        appendLine()
        appendLine("## Commands by kind")
        appendLine()
        DrawCommandKind.entries.forEach { kind ->
            appendLine("- ${kind.stableName}: ${commandCountsByKind[kind] ?: 0}")
        }
        appendLine()
        appendLine("## Transcript preview")
        appendLine()
        appendLine("```jsonl")
        appendLine(transcriptPreview())
        appendLine("```")
    }

    fun toProperties(): String = buildString {
        appendLine("name=${manifest.name}")
        appendLine("staticallyComplete=${manifest.staticallyComplete}")
        appendLine("featureCount=$featureCount")
        appendLine("commandCount=$commandCount")
        appendLine("staticDiagnostics=$staticDiagnostics")
        appendLine("commandDiagnostics=$commandDiagnostics")
        appendLine("hasErrors=$hasErrors")
        DrawCommandKind.entries.forEach { kind ->
            appendLine("commandKind.${kind.stableName}=${commandCountsByKind[kind] ?: 0}")
        }
    }

    fun transcriptPreview(): String {
        if (transcript.isBlank()) return ""
        val lines = transcript.lines().filter { it.isNotBlank() }
        val limited = lines.take(transcriptPreviewLineLimit)
        val omitted = lines.size - limited.size
        return buildString {
            limited.forEach { appendLine(it) }
            if (omitted > 0) appendLine("... $omitted more transcript line(s) omitted ...")
        }.trimEnd()
    }

    companion object {
        const val DEFAULT_TRANSCRIPT_PREVIEW_LINES: Int = 40
    }
}

/** Public helper for producing diagnostics from the facade. */
object S52Diagnostics {
    fun create(
        session: S52PortrayalSession,
        request: S52PortrayalRequest,
        name: String = "s52-kotlin-webgl-diagnostics",
        transcriptPreviewLineLimit: Int = S52DiagnosticBundle.DEFAULT_TRANSCRIPT_PREVIEW_LINES
    ): S52DiagnosticBundle {
        val result = session.portray(request)
        return fromResult(
            manifest = session.manifest(name),
            request = request,
            result = result,
            transcriptPreviewLineLimit = transcriptPreviewLineLimit
        )
    }

    fun fromResult(
        manifest: S52RuntimeManifest,
        request: S52PortrayalRequest,
        result: S52PortrayalResult,
        transcriptPreviewLineLimit: Int = S52DiagnosticBundle.DEFAULT_TRANSCRIPT_PREVIEW_LINES
    ): S52DiagnosticBundle = S52DiagnosticBundle(
        manifest = manifest,
        featureCount = request.features.size,
        commandCount = result.commands.size,
        commandCountsByKind = result.commands.countByKind(),
        staticDiagnostics = result.staticCompleteness.diagnostics.size,
        commandDiagnostics = result.commandValidation.diagnostics.size,
        transcript = result.transcript,
        transcriptPreviewLineLimit = transcriptPreviewLineLimit
    )

    private fun List<S52DrawCommand>.countByKind(): Map<DrawCommandKind, Int> {
        val counts = DrawCommandKind.entries.associateWith { 0 }.toMutableMap()
        forEach { command -> counts[command.kind] = (counts[command.kind] ?: 0) + 1 }
        return counts.toMap()
    }
}

fun S52PortrayalSession.diagnosticBundle(
    request: S52PortrayalRequest,
    name: String = "s52-kotlin-webgl-diagnostics",
    transcriptPreviewLineLimit: Int = S52DiagnosticBundle.DEFAULT_TRANSCRIPT_PREVIEW_LINES
): S52DiagnosticBundle = S52Diagnostics.create(
    session = this,
    request = request,
    name = name,
    transcriptPreviewLineLimit = transcriptPreviewLineLimit
)
