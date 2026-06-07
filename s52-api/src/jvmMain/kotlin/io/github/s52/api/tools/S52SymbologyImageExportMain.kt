package io.github.s52.api.tools

import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.LineStyleDefinition
import io.github.s52.preslib.PatternDefinition
import io.github.s52.preslib.S52Color
import io.github.s52.preslib.SymbolDefinition
import io.github.s52.preslib.VectorCommand
import io.github.s52.preslib.opencpn.OpenCpnAssetKind
import io.github.s52.preslib.opencpn.OpenCpnChartSymbolsImporter
import io.github.s52.preslib.opencpn.OpenCpnRenderableAsset
import io.github.s52.preslib.opencpn.OpenCpnRenderablePack
import io.github.s52.preslib.source.PresLibPackBuilder
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceColor
import java.io.File
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * JVM-only CI exporter for generated symbology SVGs.
 *
 * Phase 26 exports OpenCPN chartsymbols.xml assets as color-aware,
 * contour-aware, and bounds-safe SVG. It also renders line-style assets as
 * repeated samples, instead of a single generic placeholder line.
 */
object S52SymbologyImageExporter {
    const val MinimumRealSymbolCount: Int = 50

    fun exportOpenCpn(outputDirectory: File): SymbologyImageExportReport {
        val input = configuredChartsymbolsFile()
            ?: error(
                "Real OpenCPN symbology export requires -Dopencpn.chartsymbols=/path/to/chartsymbols.xml " +
                    "or OPENCPN_CHARTSYMBOLS_XML_FILE. The built-in compatibility fallback is intentionally refused."
            )
        return exportImportedOpenCpn(outputDirectory, input)
    }

    fun exportImportedOpenCpn(outputDirectory: File, chartsymbolsFile: File): SymbologyImageExportReport {
        val renderable = OpenCpnChartSymbolsImporter.importRenderableFile(chartsymbolsFile)
        require(renderable.symbols.size >= MinimumRealSymbolCount) {
            "Imported OpenCPN symbol count is too small (${renderable.symbols.size}). " +
                "This usually means a wrong or truncated chartsymbols.xml input was supplied."
        }
        return exportRenderableOpenCpn(outputDirectory, renderable, chartsymbolsFile.parentFile)
    }

    fun exportSourcePack(outputDirectory: File, sourcePack: PresLibSourcePack): SymbologyImageExportReport {
        require("import" in sourcePack.metadata.edition.lowercase()) {
            "Refusing to export non-imported S-52 symbology pack: ${sourcePack.metadata.edition}"
        }

        val pack = PresLibPackBuilder.build(sourcePack)
        val symbols = pack.symbols.all()
        val lineStyles = pack.lineStyles.all()
        val patterns = pack.patterns.all()
        val colors = pack.colors.all(S52Palette.DayBright)

        require(symbols.size >= MinimumRealSymbolCount) {
            "Imported OpenCPN symbol count is too small (${symbols.size}). " +
                "This usually means a wrong or truncated chartsymbols.xml input was supplied."
        }

        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()
        val files = mutableListOf<File>()
        val symbolDir = outputDirectory.resolve("symbols").also { it.mkdirs() }
        val lineDir = outputDirectory.resolve("lines").also { it.mkdirs() }
        val patternDir = outputDirectory.resolve("patterns").also { it.mkdirs() }
        val colorDir = outputDirectory.resolve("colors").also { it.mkdirs() }

        for (symbol in symbols) files += writeText(symbolDir.resolve("${safeFileName(symbol.name)}.svg"), renderLegacySymbolSvg(symbol))
        for (line in lineStyles) files += writeText(lineDir.resolve("${safeFileName(line.name)}.svg"), renderLegacyLineStyleSvg(line))
        for (pattern in patterns) files += writeText(patternDir.resolve("${safeFileName(pattern.name)}.svg"), renderLegacyPatternSvg(pattern))
        for (color in colors) files += writeText(colorDir.resolve("${safeFileName(color.token)}.svg"), renderColorSvg(color.token, color.r, color.g, color.b))
        files += writeText(outputDirectory.resolve("index.html"), renderIndexHtml(sourcePack.metadata.name, sourcePack.metadata.edition, symbols.map { it.name }, lineStyles.map { it.name }, patterns.map { it.name }, colors.map { it.token }))
        files += writeText(outputDirectory.resolve("manifest.properties"), manifest(sourcePack.metadata.name, sourcePack.metadata.edition, sourcePack.metadata.sourceDescription, sourcePack.metadata.generatedBy, symbols.size, lineStyles.size, patterns.size, colors.size, files.size + 1))

        return SymbologyImageExportReport("source-pack", outputDirectory, symbols.size, lineStyles.size, patterns.size, colors.size, files.size)
    }

    private fun exportRenderableOpenCpn(outputDirectory: File, renderable: OpenCpnRenderablePack, assetDirectory: File? = null): SymbologyImageExportReport {
        val sourcePack = renderable.sourcePack
        val colorMap = renderable.colorsByPalette[S52Palette.DayBright]
            .orEmpty()
            .ifEmpty { renderable.sourcePack.colorTables.firstOrNull { it.palette == S52Palette.DayBright }?.colors.orEmpty() }
            .associateBy { it.token.uppercase() }

        val pack = PresLibPackBuilder.build(sourcePack)
        val colors = pack.colors.all(S52Palette.DayBright)

        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()
        val files = mutableListOf<File>()
        val symbolDir = outputDirectory.resolve("symbols").also { it.mkdirs() }
        val lineDir = outputDirectory.resolve("lines").also { it.mkdirs() }
        val patternDir = outputDirectory.resolve("patterns").also { it.mkdirs() }
        val colorDir = outputDirectory.resolve("colors").also { it.mkdirs() }

        for (symbol in renderable.symbols) files += writeText(symbolDir.resolve("${safeFileName(symbol.name)}.svg"), renderOpenCpnAssetSvg(symbol, colorMap))
        for (line in renderable.lineStyles) files += writeText(lineDir.resolve("${safeFileName(line.name)}.svg"), renderOpenCpnLineStyleSvg(line, colorMap))
        for (pattern in renderable.patterns) files += writeText(patternDir.resolve("${safeFileName(pattern.name)}.svg"), renderOpenCpnAssetSvg(pattern, colorMap))
        for (color in colors) files += writeText(colorDir.resolve("${safeFileName(color.token)}.svg"), renderColorSvg(color.token, color.r, color.g, color.b))

        val atlasFiles = copyOpenCpnRasterAtlases(assetDirectory, outputDirectory)

        files += writeText(outputDirectory.resolve("index.html"), renderIndexHtml(sourcePack.metadata.name, sourcePack.metadata.edition, renderable.symbols.map { it.name }, renderable.lineStyles.map { it.name }, renderable.patterns.map { it.name }, colors.map { it.token }, atlasFiles.map { it.name }))
        files += writeText(outputDirectory.resolve("manifest.properties"), manifest(sourcePack.metadata.name, sourcePack.metadata.edition, sourcePack.metadata.sourceDescription, sourcePack.metadata.generatedBy, renderable.symbols.size, renderable.lineStyles.size, renderable.patterns.size, colors.size, files.size + atlasFiles.size + 1, atlasFiles.map { it.name }))

        return SymbologyImageExportReport("opencpn-chartsymbols", outputDirectory, renderable.symbols.size, renderable.lineStyles.size, renderable.patterns.size, colors.size, files.size + atlasFiles.size)
    }

    private fun configuredChartsymbolsFile(): File? =
        System.getProperty("opencpn.chartsymbols")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: System.getenv("OPENCPN_CHARTSYMBOLS_XML_FILE")?.takeIf { it.isNotBlank() }?.let(::File)

    private fun renderOpenCpnAssetSvg(asset: OpenCpnRenderableAsset, colors: Map<String, SourceColor>): String {
        val rendered = HpglSvgRenderer(asset, colors).render()
        val title = when (asset.kind) {
            OpenCpnAssetKind.Symbol -> "Symbol"
            OpenCpnAssetKind.LineStyle -> "Line style"
            OpenCpnAssetKind.Pattern -> "Pattern"
        }
        return """
            |<svg xmlns="http://www.w3.org/2000/svg" width="${rendered.width}" height="${rendered.height}" viewBox="0 0 ${rendered.width} ${rendered.height}" overflow="visible" shape-rendering="geometricPrecision" role="img" aria-label="${xml(asset.name)}">
            |  <title>${xml(asset.name)} $title</title>
            |  <desc>colorRefs=${xml(asset.colorRefs.joinToString(","))}; unresolvedColors=${xml(rendered.unresolvedColorTokens.joinToString(","))}</desc>
            |  <rect width="100%" height="100%" fill="white"/>
            |  <g transform="translate(${rendered.offsetX} ${rendered.offsetY})">
            |${rendered.elements.joinToString("\n") { "    $it" }}
            |  </g>
            |  <text x="4" y="${rendered.height - 4}" font-family="monospace" font-size="8" fill="#333333">${xml(asset.name)} $title</text>
            |</svg>
        """.trimMargin()
    }

    private fun renderOpenCpnLineStyleSvg(asset: OpenCpnRenderableAsset, colors: Map<String, SourceColor>): String {
        val rendered = HpglSvgRenderer(asset, colors).render()
        val width = 420.0
        val height = 110.0
        val tileMaxWidth = 58.0
        val tileMaxHeight = 42.0
        val scale = min(1.0, min(tileMaxWidth / rendered.width, tileMaxHeight / rendered.height))
        val step = max(54.0, rendered.width * scale + 12.0)
        val copies = max(1, ((width - 32.0) / step).toInt())
        val groups = (0 until copies).joinToString("\n") { index ->
            val x = 16.0 + index * step
            val y = 24.0
            """
              |  <g class="line-style-tile" transform="translate($x $y) scale($scale) translate(${rendered.offsetX} ${rendered.offsetY})">
              |${rendered.elements.joinToString("\n") { "    $it" }}
              |  </g>
            """.trimMargin()
        }
        return """
            |<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height" overflow="visible" shape-rendering="geometricPrecision" role="img" aria-label="${xml(asset.name)}">
            |  <rect width="100%" height="100%" fill="white"/>
            |  <line x1="12" y1="48" x2="408" y2="48" stroke="#D0D0D0" stroke-width="1" stroke-dasharray="4 4"/>
            |$groups
            |  <text x="12" y="94" font-family="monospace" font-size="12" fill="#333333">${xml(asset.name)} repeated line-style sample</text>
            |</svg>
        """.trimMargin()
    }

    private fun renderLegacySymbolSvg(symbol: SymbolDefinition): String {
        val width = max(32.0, symbol.width + 24.0)
        val height = max(32.0, symbol.height + 24.0)
        val path = symbol.commands.toSvgPath(width / 2.0 - symbol.pivotX, height / 2.0 - symbol.pivotY)
        return """
            |<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height" role="img" aria-label="${xml(symbol.name)}">
            |  <rect width="100%" height="100%" fill="white"/>
            |  <path d="$path" fill="none" stroke="#000000" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            |  <text x="4" y="${height - 4}" font-family="monospace" font-size="8" fill="#333333">${xml(symbol.name)}</text>
            |</svg>
        """.trimMargin()
    }

    private fun renderLegacyLineStyleSvg(line: LineStyleDefinition): String = """
        |<svg xmlns="http://www.w3.org/2000/svg" width="320" height="80" viewBox="0 0 320 80" role="img" aria-label="${xml(line.name)}">
        |  <rect width="100%" height="100%" fill="white"/>
        |  <line x1="20" y1="34" x2="300" y2="34" stroke="#000000" stroke-width="4" stroke-linecap="round"/>
        |  <text x="20" y="64" font-family="monospace" font-size="14" fill="#333333">${xml(line.name)}</text>
        |</svg>
    """.trimMargin()

    private fun renderLegacyPatternSvg(pattern: PatternDefinition): String = """
        |<svg xmlns="http://www.w3.org/2000/svg" width="160" height="120" viewBox="0 0 160 120" role="img" aria-label="${xml(pattern.name)}">
        |  <defs><pattern id="hatch" width="12" height="12" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><line x1="0" y1="0" x2="0" y2="12" stroke="#000000" stroke-width="2"/></pattern></defs>
        |  <rect x="12" y="10" width="136" height="76" fill="#eef6ff" stroke="#000000" stroke-width="1"/>
        |  <rect x="12" y="10" width="136" height="76" fill="url(#hatch)" opacity="0.65"/>
        |  <text x="12" y="108" font-family="monospace" font-size="12" fill="#333333">${xml(pattern.name)}</text>
        |</svg>
    """.trimMargin()

    private fun renderColorSvg(token: String, r: Int, g: Int, b: Int): String {
        val hex = rgbHex(r, g, b)
        return """
            |<svg xmlns="http://www.w3.org/2000/svg" width="180" height="110" viewBox="0 0 180 110" role="img" aria-label="${xml(token)}">
            |  <rect width="100%" height="100%" fill="white"/>
            |  <rect x="16" y="12" width="148" height="56" fill="$hex" stroke="#000000"/>
            |  <text x="16" y="88" font-family="monospace" font-size="14" fill="#000000">${xml(token)} $hex</text>
            |</svg>
        """.trimMargin()
    }

    private fun manifest(name: String, edition: String, sourceDescription: String, generatedBy: String, symbols: Int, lines: Int, patterns: Int, colors: Int, imageFiles: Int, pngAtlases: List<String> = emptyList()): String = buildString {
        appendLine("name=$name")
        appendLine("edition=$edition")
        appendLine("sourceDescription=$sourceDescription")
        appendLine("generatedBy=$generatedBy")
        appendLine("symbols=$symbols")
        appendLine("lines=$lines")
        appendLine("patterns=$patterns")
        appendLine("colors=$colors")
        appendLine("imageFiles=$imageFiles")
        appendLine("synthetic=false")
        appendLine("svgColorAware=true")
        appendLine("svgContourAware=true")
        appendLine("svgBoundsAware=true")
        appendLine("lineStyleSampleRepeated=true")
        appendLine("hpglArcCenterAware=true")
            appendLine("svgPenLetterColorAware=true")
            appendLine("svgColorRefDiagnostics=true")
            appendLine("svgCompactColorRefAware=true")
        appendLine("pngSymbolAtlases=${pngAtlases.size}")
        if (pngAtlases.isNotEmpty()) appendLine("pngSymbolAtlasFiles=${pngAtlases.joinToString(",")}")
    }

    private fun renderIndexHtml(metadataName: String, metadataEdition: String, symbols: List<String>, lineStyles: List<String>, patterns: List<String>, colors: List<String>, pngAtlases: List<String> = emptyList()): String = buildString {
        appendLine("<!doctype html>")
        appendLine("<html lang=\"en\"><head><meta charset=\"utf-8\"/><title>S-52 symbology images</title>")
        appendLine("<style>body{font-family:system-ui,sans-serif;margin:24px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:16px}img{max-width:100%;border:1px solid #ddd;background:white}.section{margin-top:32px}</style>")
        appendLine("</head><body>")
        appendLine("<h1>S-52 symbology images</h1>")
        appendLine("<p>Pack: ${xml(metadataName)} / ${xml(metadataEdition)}</p>")
        if (pngAtlases.isNotEmpty()) appendAtlasSection(pngAtlases)
        appendGallerySection("Symbols", "symbols", symbols)
        appendGallerySection("Line styles", "lines", lineStyles)
        appendGallerySection("Patterns", "patterns", patterns)
        appendGallerySection("Colors", "colors", colors)
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


    private fun StringBuilder.appendAtlasSection(pngAtlases: List<String>) {
        appendLine("""
            |<div class="section"><h2>PNG symbol atlases (${pngAtlases.size})</h2><div class="grid">
        """.trimMargin())
        for (fileName in pngAtlases) {
            appendLine("""
                |<figure><a href="${xml(fileName)}"><img src="${xml(fileName)}" alt="${xml(fileName)}"/></a><figcaption>${xml(fileName)}</figcaption></figure>
            """.trimMargin())
        }
        appendLine("</div></div>")
    }

    private fun copyOpenCpnRasterAtlases(assetDirectory: File?, outputDirectory: File): List<File> {
        if (assetDirectory == null || !assetDirectory.isDirectory) return emptyList()
        val requested = listOf(
            "rastersymbols-day.png" to "symbol-atlas-day.png",
            "rastersymbols-dusk.png" to "symbol-atlas-dusk.png",
            "rastersymbols-dark.png" to "symbol-atlas-dark.png"
        )
        val copied = mutableListOf<File>()
        for ((sourceName, targetName) in requested) {
            val source = assetDirectory.resolve(sourceName)
            if (source.isFile) {
                val target = outputDirectory.resolve(targetName)
                target.parentFile?.mkdirs()
                Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                copied += target
            }
        }
        return copied
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

    private fun rgbHex(r: Int, g: Int, b: Int): String = "#%02X%02X%02X".format(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    private fun safeFileName(name: String): String = name.uppercase().replace(Regex("[^A-Z0-9_.-]"), "_")
    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private class HpglSvgRenderer(
        private val asset: OpenCpnRenderableAsset,
        private val colors: Map<String, SourceColor>
    ) {
        private var currentX = 0.0
        private var currentY = 0.0
        private var penDown = false
        private var strokeWidth = 1.5
        private var dashArray: String? = null
        private val elements = mutableListOf<String>()
        private val unresolvedColorTokens = mutableListOf<String>()
        private var currentColor = resolveColor(null)
        private var currentPath = StringBuilder()
        private var polygonPath = StringBuilder()
        private var polygonMode = false
        private val bounds = MutableBounds()
        private var maxStrokeWidth = strokeWidth

        fun render(): RenderedSvg {
            for (token in tokenize(asset.hpgl)) {
                when (token.code) {
                    "SP" -> {
                        flushCurrentPath()
                        currentColor = resolveColor(token.args)
                    }
                    "SW" -> {
                        flushCurrentPath()
                        strokeWidth = token.args.toDoubleOrNull()?.coerceAtLeast(0.5) ?: strokeWidth
                        maxStrokeWidth = max(maxStrokeWidth, strokeWidth)
                    }
                    "LT" -> {
                        flushCurrentPath()
                        dashArray = dashArrayFor(token.args)
                    }
                    "PU" -> handleMove(token.args, draw = false)
                    "PD" -> handleMove(token.args, draw = true)
                    "CI" -> drawCircle(token.args.toDoubleOrNull() ?: 4.0)
                    "AA" -> drawArc(token.args)
                    "PM" -> handlePolygonMode(token.args)
                    "FP" -> fillPolygon(clear = false)
                    "EP" -> edgePolygon(clear = true)
                    "EA" -> edgeRectangle(token.args, relative = false)
                    "RA" -> fillRectangle(token.args, relative = false)
                    "ER" -> edgeRectangle(token.args, relative = true)
                    "RR" -> fillRectangle(token.args, relative = true)
                    "FT", "SC", "AP", "AC", "ST" -> Unit
                }
            }
            flushCurrentPath()
            if (elements.isEmpty()) addFallbackElement()
            val padding = max(12.0, maxStrokeWidth * 2.0 + 8.0)
            val width = ceil(max(32.0, bounds.width + padding * 2.0))
            val height = ceil(max(32.0, bounds.height + padding * 2.0 + 12.0))
            return RenderedSvg(width, height, padding - bounds.minX, padding - bounds.minY, elements, unresolvedColorTokens.sorted().distinct())
        }

        private fun handleMove(args: String, draw: Boolean) {
            val points = parseCoordinatePairs(args)
            if (points.isEmpty()) {
                penDown = draw
                return
            }
            for (point in points) {
                if (draw) lineTo(point.first, point.second) else moveTo(point.first, point.second)
            }
            penDown = draw
        }

        private fun moveTo(x: Double, y: Double) {
            currentX = x
            currentY = y
            bounds.add(x, y, strokeWidth)
            if (polygonMode) polygonPath.append("M $x $y ") else currentPath.append("M $x $y ")
        }

        private fun lineTo(x: Double, y: Double) {
            if (!penDown && currentPath.isEmpty() && !polygonMode) currentPath.append("M $currentX $currentY ")
            if (polygonMode && polygonPath.isEmpty()) polygonPath.append("M $currentX $currentY ")
            val target = if (polygonMode) polygonPath else currentPath
            target.append("L $x $y ")
            currentX = x
            currentY = y
            bounds.add(x, y, strokeWidth)
        }

        private fun drawCircle(radius: Double) {
            flushCurrentPath()
            val r = max(1.0, radius)
            bounds.add(currentX - r, currentY - r, strokeWidth)
            bounds.add(currentX + r, currentY + r, strokeWidth)
            elements += "<circle cx=\"$currentX\" cy=\"$currentY\" r=\"$r\" fill=\"none\" stroke=\"$currentColor\" stroke-width=\"$strokeWidth\"${dashAttr()}/>"
        }

        private fun drawArc(args: String) {
            val nums = parseNumbers(args)
            if (nums.size < 3) return
            val centerX = nums[0]
            val centerY = nums[1]
            val sweepDeg = nums[2]
            val radius = max(1.0, hypot(currentX - centerX, currentY - centerY))
            val startAngle = atan2(currentY - centerY, currentX - centerX)
            val sweep = Math.toRadians(sweepDeg)
            val steps = min(64, max(4, abs(sweepDeg / 8.0).toInt()))
            for (i in 1..steps) {
                val angle = startAngle + sweep * i / steps
                lineTo(centerX + cos(angle) * radius, centerY + sin(angle) * radius)
            }
        }

        private fun handlePolygonMode(args: String) {
            when (args.trim().toIntOrNull() ?: 0) {
                0 -> {
                    flushCurrentPath()
                    polygonMode = true
                    polygonPath = StringBuilder().append("M $currentX $currentY ")
                }
                1 -> {
                    polygonPath.append("Z ")
                    polygonPath.append("M $currentX $currentY ")
                }
                2 -> {
                    polygonPath.append("Z ")
                    polygonMode = false
                }
                else -> Unit
            }
        }

        private fun fillPolygon(clear: Boolean) {
            val d = polygonPath.toString().trim()
            if (d.isNotBlank()) elements += "<path d=\"$d\" fill=\"$currentColor\" stroke=\"none\" fill-rule=\"evenodd\"/>"
            if (clear) polygonPath = StringBuilder()
        }

        private fun edgePolygon(clear: Boolean) {
            val d = polygonPath.toString().trim()
            if (d.isNotBlank()) elements += "<path d=\"$d\" fill=\"none\" stroke=\"$currentColor\" stroke-width=\"$strokeWidth\" stroke-linecap=\"round\" stroke-linejoin=\"round\"${dashAttr()}/>"
            polygonMode = false
            if (clear) polygonPath = StringBuilder()
        }

        private fun fillRectangle(args: String, relative: Boolean) {
            rectangle(args, relative, fill = true)
        }

        private fun edgeRectangle(args: String, relative: Boolean) {
            rectangle(args, relative, fill = false)
        }

        private fun rectangle(args: String, relative: Boolean, fill: Boolean) {
            flushCurrentPath()
            val nums = parseNumbers(args)
            if (nums.size < 2) return
            val x2 = if (relative) currentX + nums[0] else nums[0]
            val y2 = if (relative) currentY + nums[1] else nums[1]
            val x1 = currentX
            val y1 = currentY
            bounds.add(x1, y1, strokeWidth)
            bounds.add(x2, y2, strokeWidth)
            val left = min(x1, x2)
            val top = min(y1, y2)
            val w = abs(x2 - x1)
            val h = abs(y2 - y1)
            if (fill) {
                elements += "<rect x=\"$left\" y=\"$top\" width=\"$w\" height=\"$h\" fill=\"$currentColor\" stroke=\"none\"/>"
            } else {
                elements += "<rect x=\"$left\" y=\"$top\" width=\"$w\" height=\"$h\" fill=\"none\" stroke=\"$currentColor\" stroke-width=\"$strokeWidth\"${dashAttr()}/>"
            }
        }

        private fun flushCurrentPath() {
            val d = currentPath.toString().trim()
            if (d.isNotBlank()) {
                elements += "<path d=\"$d\" fill=\"none\" stroke=\"$currentColor\" stroke-width=\"$strokeWidth\" stroke-linecap=\"round\" stroke-linejoin=\"round\"${dashAttr()}/>"
                currentPath = StringBuilder()
            }
        }

        private fun addFallbackElement() {
            bounds.add(-8.0, -8.0, strokeWidth)
            bounds.add(8.0, 8.0, strokeWidth)
            elements += "<path d=\"M 0 -8 L 8 8 L -8 8 Z\" fill=\"none\" stroke=\"$currentColor\" stroke-width=\"1.5\"/>"
        }

        private fun resolveColor(arg: String?): String {
            val token = colorToken(arg)
            val color = colors[token.uppercase()]
            if (color != null) return rgbHex(color.r, color.g, color.b)
            unresolvedColorTokens += token
            return fallbackVisibleColorForPen(arg)
        }

        private fun colorToken(arg: String?): String {
            val raw = arg.orEmpty().trim().uppercase()
            if (raw.isBlank()) return asset.colorRefs.firstOrNull() ?: "CHBLK"

            raw.toIntOrNull()?.let { index ->
                return colorRefByIndex(if (index <= 0) 0 else index - 1)
            }

            if (raw.length == 1 && raw[0] in 'A'..'Z') {
                return colorRefByIndex(raw[0] - 'A')
            }

            val known = colors.keys
            if (raw in known) return raw
            if (raw.length > 4 && raw.drop(1) in known) return raw.drop(1)

            return raw.take(8)
        }

        private fun colorRefByIndex(index: Int): String =
            asset.colorRefs.getOrNull(index)
                ?: standardPenColorRefs().getOrNull(index)
                ?: asset.colorRefs.firstOrNull()
                ?: "CHBLK"

        private fun standardPenColorRefs(): List<String> = listOf(
            "CHBLK", "CHRED", "CHGRN", "CHYLW", "CHMGD", "CHBRN", "CHWHT", "CHGRD"
        )

        private fun fallbackVisibleColorForPen(arg: String?): String = when (arg.orEmpty().trim().uppercase()) {
            "1", "A", "" -> "#000000"
            "2", "B" -> "#C02020"
            "3", "C" -> "#208040"
            "4", "D" -> "#2040C0"
            "5", "E" -> "#B08000"
            else -> "#000000"
        }

        private fun dashArrayFor(args: String): String? {
            val nums = parseNumbers(args)
            if (nums.isEmpty()) return null
            return when (nums[0].toInt()) {
                0 -> null
                1 -> "8 4"
                2 -> "12 6"
                3 -> "4 4"
                4 -> "2 4"
                else -> "6 4"
            }
        }

        private fun dashAttr(): String = dashArray?.let { " stroke-dasharray=\"$it\"" }.orEmpty()
    }

    private data class RenderedSvg(val width: Double, val height: Double, val offsetX: Double, val offsetY: Double, val elements: List<String>, val unresolvedColorTokens: List<String> = emptyList())

    private class MutableBounds {
        var minX: Double = Double.POSITIVE_INFINITY
        var minY: Double = Double.POSITIVE_INFINITY
        var maxX: Double = Double.NEGATIVE_INFINITY
        var maxY: Double = Double.NEGATIVE_INFINITY
        val width: Double get() = if (minX.isFinite() && maxX.isFinite()) max(1.0, maxX - minX) else 16.0
        val height: Double get() = if (minY.isFinite() && maxY.isFinite()) max(1.0, maxY - minY) else 16.0
        fun add(x: Double, y: Double, strokeWidth: Double = 0.0) {
            val pad = strokeWidth / 2.0
            minX = min(minX, x - pad)
            minY = min(minY, y - pad)
            maxX = max(maxX, x + pad)
            maxY = max(maxY, y + pad)
        }
    }
}

private data class HpglToken(val code: String, val args: String)

private fun tokenize(hpgl: String): List<HpglToken> {
    val tokens = mutableListOf<HpglToken>()
    var index = 0
    while (index < hpgl.length - 1) {
        val a = hpgl[index]
        val b = hpgl[index + 1]
        if (a.isLetter() && b.isLetter()) {
            val code = "${a.uppercaseChar()}${b.uppercaseChar()}"
            var end = index + 2
            while (end < hpgl.length && hpgl[end] != ';') end++
            tokens += HpglToken(code, hpgl.substring(index + 2, end).trim())
            index = if (end < hpgl.length) end + 1 else end
        } else {
            index++
        }
    }
    return tokens
}

private fun parseCoordinatePairs(args: String): List<Pair<Double, Double>> {
    val nums = parseNumbers(args)
    val result = mutableListOf<Pair<Double, Double>>()
    var i = 0
    while (i + 1 < nums.size) {
        result += nums[i] to nums[i + 1]
        i += 2
    }
    return result
}

private fun parseNumbers(args: String): List<Double> =
    Regex("""[-+]?\d+(?:\.\d+)?""").findAll(args).map { it.value.toDouble() }.toList()

data class SymbologyImageExportReport(
    val sourceKind: String,
    val outputDirectory: File,
    val symbolCount: Int,
    val lineStyleCount: Int,
    val patternCount: Int,
    val colorCount: Int,
    val fileCount: Int
) {
    override fun toString(): String =
        "source=$sourceKind symbols=$symbolCount lines=$lineStyleCount patterns=$patternCount colors=$colorCount files=$fileCount output=${outputDirectory.absolutePath}"
}

fun main(args: Array<String>) {
    val output = File(args.getOrNull(0) ?: "build/s52-symbology-images")
    val input = args.getOrNull(1)?.let(::File)
        ?: System.getProperty("opencpn.chartsymbols")?.let(::File)
        ?: System.getenv("OPENCPN_CHARTSYMBOLS_XML_FILE")?.let(::File)
        ?: error("Missing OpenCPN chartsymbols.xml file. Pass outputDir inputFile, -Dopencpn.chartsymbols, or OPENCPN_CHARTSYMBOLS_XML_FILE.")
    val report = S52SymbologyImageExporter.exportImportedOpenCpn(output, input)
    println(report)
}
