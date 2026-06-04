package io.github.s52.api.tools

import io.github.s52.preslib.LineStyleDefinition
import io.github.s52.preslib.PatternDefinition
import io.github.s52.preslib.S52Color
import io.github.s52.preslib.SymbolDefinition
import io.github.s52.preslib.VectorCommand
import io.github.s52.preslib.s52lib.S52LibCompatPresLib
import java.io.File
import kotlin.math.max

/**
 * JVM-only Phase 21 exporter for CI artifacts.
 *
 * It intentionally exports from [S52LibCompatPresLib], not from
 * PresLibPack.phase2Synthetic(), so the artifact folder reflects the real
 * s52lib-compatible pack configured for the library. When a fuller imported
 * s52lib/IHO-compatible pack is wired into S52LibCompatPresLib, this exporter
 * will emit every asset from that pack without changing CI.
 */
object S52SymbologyImageExporter {
    fun exportS52LibCompat(outputDirectory: File): SymbologyImageExportReport {
        val sourcePack = S52LibCompatPresLib.sourcePack()
        require("s52lib" in sourcePack.metadata.edition.lowercase()) {
            "Refusing to export symbology images from non-s52lib pack: ${sourcePack.metadata.edition}"
        }

        val pack = S52LibCompatPresLib.pack()
        val symbols = pack.symbols.all()
        val lineStyles = pack.lineStyles.all()
        val patterns = pack.patterns.all()
        val colors = pack.colors.all(io.github.s52.core.settings.S52Palette.DayBright)

        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        val files = mutableListOf<File>()
        val symbolDir = outputDirectory.resolve("symbols").also { it.mkdirs() }
        val lineDir = outputDirectory.resolve("lines").also { it.mkdirs() }
        val patternDir = outputDirectory.resolve("patterns").also { it.mkdirs() }
        val colorDir = outputDirectory.resolve("colors").also { it.mkdirs() }

        for (symbol in symbols) files += writeText(symbolDir.resolve("${safeFileName(symbol.name)}.svg"), renderSymbolSvg(symbol))
        for (line in lineStyles) files += writeText(lineDir.resolve("${safeFileName(line.name)}.svg"), renderLineStyleSvg(line))
        for (pattern in patterns) files += writeText(patternDir.resolve("${safeFileName(pattern.name)}.svg"), renderPatternSvg(pattern))
        for (color in colors) files += writeText(colorDir.resolve("${safeFileName(color.token)}.svg"), renderColorSvg(color))

        val index = renderIndexHtml(
            metadataName = sourcePack.metadata.name,
            metadataEdition = sourcePack.metadata.edition,
            symbols = symbols,
            lineStyles = lineStyles,
            patterns = patterns,
            colors = colors
        )
        files += writeText(outputDirectory.resolve("index.html"), index)

        val manifest = buildString {
            appendLine("name=${sourcePack.metadata.name}")
            appendLine("edition=${sourcePack.metadata.edition}")
            appendLine("sourceDescription=${sourcePack.metadata.sourceDescription}")
            appendLine("generatedBy=${sourcePack.metadata.generatedBy}")
            appendLine("symbols=${symbols.size}")
            appendLine("lines=${lineStyles.size}")
            appendLine("patterns=${patterns.size}")
            appendLine("colors=${colors.size}")
            appendLine("imageFiles=${files.size + 1}")
            appendLine("synthetic=false")
        }
        files += writeText(outputDirectory.resolve("manifest.properties"), manifest)

        return SymbologyImageExportReport(
            outputDirectory = outputDirectory,
            symbolCount = symbols.size,
            lineStyleCount = lineStyles.size,
            patternCount = patterns.size,
            colorCount = colors.size,
            fileCount = files.size
        )
    }

    private fun renderSymbolSvg(symbol: SymbolDefinition): String {
        val width = max(32.0, symbol.width + 24.0)
        val height = max(32.0, symbol.height + 24.0)
        val path = symbol.commands.toSvgPath(offsetX = width / 2.0 - symbol.pivotX, offsetY = height / 2.0 - symbol.pivotY)
        return """
            |<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height" role="img" aria-label="${xml(symbol.name)}">
            |  <rect width="100%" height="100%" fill="white"/>
            |  <path d="$path" fill="none" stroke="#000000" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            |  <text x="4" y="${height - 4}" font-family="monospace" font-size="8" fill="#333333">${xml(symbol.name)}</text>
            |</svg>
        """.trimMargin()
    }

    private fun renderLineStyleSvg(line: LineStyleDefinition): String = """
        |<svg xmlns="http://www.w3.org/2000/svg" width="320" height="80" viewBox="0 0 320 80" role="img" aria-label="${xml(line.name)}">
        |  <rect width="100%" height="100%" fill="white"/>
        |  <line x1="20" y1="34" x2="300" y2="34" stroke="#000000" stroke-width="4" stroke-linecap="round"/>
        |  <text x="20" y="64" font-family="monospace" font-size="14" fill="#333333">${xml(line.name)}</text>
        |</svg>
    """.trimMargin()

    private fun renderPatternSvg(pattern: PatternDefinition): String = """
        |<svg xmlns="http://www.w3.org/2000/svg" width="160" height="120" viewBox="0 0 160 120" role="img" aria-label="${xml(pattern.name)}">
        |  <defs>
        |    <pattern id="hatch" width="12" height="12" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
        |      <line x1="0" y1="0" x2="0" y2="12" stroke="#000000" stroke-width="2"/>
        |    </pattern>
        |  </defs>
        |  <rect x="12" y="10" width="136" height="76" fill="#eef6ff" stroke="#000000" stroke-width="1"/>
        |  <rect x="12" y="10" width="136" height="76" fill="url(#hatch)" opacity="0.65"/>
        |  <text x="12" y="108" font-family="monospace" font-size="12" fill="#333333">${xml(pattern.name)}</text>
        |</svg>
    """.trimMargin()

    private fun renderColorSvg(color: S52Color): String {
        val hex = "#%02X%02X%02X".format(color.r, color.g, color.b)
        return """
            |<svg xmlns="http://www.w3.org/2000/svg" width="180" height="110" viewBox="0 0 180 110" role="img" aria-label="${xml(color.token)}">
            |  <rect width="100%" height="100%" fill="white"/>
            |  <rect x="16" y="12" width="148" height="56" fill="$hex" stroke="#000000"/>
            |  <text x="16" y="88" font-family="monospace" font-size="14" fill="#000000">${xml(color.token)} $hex</text>
            |</svg>
        """.trimMargin()
    }

    private fun renderIndexHtml(
        metadataName: String,
        metadataEdition: String,
        symbols: List<SymbolDefinition>,
        lineStyles: List<LineStyleDefinition>,
        patterns: List<PatternDefinition>,
        colors: List<S52Color>
    ): String = buildString {
        appendLine("<!doctype html>")
        appendLine("<html lang=\"en\"><head><meta charset=\"utf-8\"/><title>S-52 symbology images</title>")
        appendLine("<style>body{font-family:system-ui,sans-serif;margin:24px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:16px}img{max-width:100%;border:1px solid #ddd;background:white}.section{margin-top:32px}</style>")
        appendLine("</head><body>")
        appendLine("<h1>S-52 symbology images</h1>")
        appendLine("<p>Pack: ${xml(metadataName)} / ${xml(metadataEdition)}</p>")
        appendGallerySection("Symbols", "symbols", symbols.map { it.name })
        appendGallerySection("Line styles", "lines", lineStyles.map { it.name })
        appendGallerySection("Patterns", "patterns", patterns.map { it.name })
        appendGallerySection("Colors", "colors", colors.map { it.token })
        appendLine("</body></html>")
    }

    private fun StringBuilder.appendGallerySection(title: String, folder: String, names: List<String>) {
        appendLine("<div class=\"section\"><h2>${xml(title)} (${names.size})</h2><div class=\"grid\">")
        for (name in names) {
            val path = "$folder/${safeFileName(name)}.svg"
            appendLine("<figure><img src=\"${xml(path)}\" alt=\"${xml(name)}\"/><figcaption>${xml(name)}</figcaption></figure>")
        }
        appendLine("</div></div>")
    }

    private fun List<VectorCommand>.toSvgPath(offsetX: Double, offsetY: Double): String = buildString {
        for (command in this@toSvgPath) {
            when (command) {
                is VectorCommand.MoveTo -> append("M ${command.x + offsetX} ${command.y + offsetY} ")
                is VectorCommand.LineTo -> append("L ${command.x + offsetX} ${command.y + offsetY} ")
                VectorCommand.ClosePath -> append("Z ")
            }
        }
    }.trim()

    private fun writeText(file: File, text: String): File {
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    private fun safeFileName(name: String): String = name.uppercase().replace(Regex("[^A-Z0-9_.-]"), "_")

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

data class SymbologyImageExportReport(
    val outputDirectory: File,
    val symbolCount: Int,
    val lineStyleCount: Int,
    val patternCount: Int,
    val colorCount: Int,
    val fileCount: Int
) {
    override fun toString(): String =
        "symbols=$symbolCount lines=$lineStyleCount patterns=$patternCount colors=$colorCount files=$fileCount output=${outputDirectory.absolutePath}"
}

fun main(args: Array<String>) {
    val output = File(args.firstOrNull() ?: "build/s52-symbology-images")
    val report = S52SymbologyImageExporter.exportS52LibCompat(output)
    println(report)
}
