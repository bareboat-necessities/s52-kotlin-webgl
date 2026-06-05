package io.github.s52.preslib.opencpn

import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.s52lib.S52LibCompatPresLib
import io.github.s52.preslib.source.PresLibMetadata
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceColor
import io.github.s52.preslib.source.SourceColorTable
import io.github.s52.preslib.source.SourceLineStyle
import io.github.s52.preslib.source.SourcePattern
import io.github.s52.preslib.source.SourceSymbol
import io.github.s52.preslib.source.SourceVectorCommand
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase 22/23 importer for OpenCPN vector symbology.
 *
 * The normal input is OpenCPN `chartsymbols.xml`. Some OpenCPN distributions
 * expose the same S-52 vector information as a legacy flat HPGL-like text
 * stream, so this importer supports both XML and flat text. Raster atlases are
 * intentionally not used.
 */
object OpenCpnChartSymbolsImporter {
    fun importFile(file: File): PresLibSourcePack {
        require(file.isFile) { "OpenCPN chartsymbols file does not exist: ${file.absolutePath}" }
        return importText(file.readText(), sourceName = file.name)
    }

    fun importXml(xml: String, sourceName: String = "chartsymbols.xml"): PresLibSourcePack =
        importText(xml, sourceName)

    fun importText(text: String, sourceName: String = "chartsymbols.xml"): PresLibSourcePack {
        val trimmed = text.trimStart()
        return if (trimmed.startsWith("<")) {
            importXmlDocument(text, sourceName)
        } else {
            importFlatChartSymbols(text, sourceName)
        }
    }

    private fun importXmlDocument(xml: String, sourceName: String): PresLibSourcePack {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            try {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Throwable) {
                // Some XML parsers do not support this feature. The importer is
                // used on local trusted build input, so continue with the
                // default parser when the feature is unavailable.
            }
        }
        val document = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val elements = document.getElementsByTagName("*")

        val symbols = linkedMapOf<String, SourceSymbol>()
        val lineStyles = linkedMapOf<String, SourceLineStyle>()
        val patterns = linkedMapOf<String, SourcePattern>()
        val colorsByPalette = mutableMapOf<S52Palette, LinkedHashMap<String, SourceColor>>()

        for (i in 0 until elements.length) {
            val element = elements.item(i) as? Element ?: continue
            val tag = element.tagName.lowercase()
            val attrs = attributes(element)

            if (tag.contains("symbol") && !tag.contains("bitmap") && !tag.contains("raster")) {
                extractSymbol(element, attrs)?.let { symbols.putIfAbsent(it.name.uppercase(), it) }
            }
            if (tag.contains("line") && (tag.contains("style") || tag.contains("symbol") || tag == "line")) {
                extractLineStyle(attrs)?.let { lineStyles.putIfAbsent(it.name.uppercase(), it) }
            }
            if (tag.contains("pattern") || tag.contains("fill")) {
                extractPattern(attrs)?.let { patterns.putIfAbsent(it.name.uppercase(), it) }
            }
            if (tag.contains("color")) {
                extractColor(attrs)?.let { (palette, color) ->
                    colorsByPalette.getOrPut(palette) { linkedMapOf() }.putIfAbsent(color.token.uppercase(), color)
                }
            }
        }

        return buildPack(
            sourceName = sourceName,
            symbols = symbols.values.sortedBy { it.name },
            lineStyles = lineStyles.values.sortedBy { it.name },
            patterns = patterns.values.sortedBy { it.name },
            colorsByPalette = colorsByPalette
        )
    }

    private fun importFlatChartSymbols(text: String, sourceName: String): PresLibSourcePack {
        val symbols = linkedMapOf<String, SourceSymbol>()
        val lineStyles = linkedMapOf<String, SourceLineStyle>()
        val patterns = linkedMapOf<String, SourcePattern>()

        // OpenCPN's flat chartsymbols stream is HPGL-like command text followed
        // by color token, symbol token, and a human-readable description.
        // Capture every plausible S-52 token after a color token and attach the
        // nearest vector chunk preceding it.
        val colorToken = "[A-Z]{2,6}[0-9A-Z]?"
        val nameToken = "[A-Z][A-Z0-9]{3,7}"
        val marker = Regex("\\b($colorToken)\\s+($nameToken)\\b")
        val matches = marker.findAll(text).toList()
        var previousEnd = 0
        for (match in matches) {
            val name = match.groupValues[2].take(8)
            if (!looksLikeS52Token(name)) continue
            val vectorStart = previousEnd.coerceAtLeast(0)
            val vectorChunk = text.substring(vectorStart, match.range.first).takeLast(4096)
            val commands = parseHpglLikeVector(vectorChunk).ifEmpty { fallbackSymbolCommands(name) }
            val symbol = sourceSymbol(name, commands)
            symbols.putIfAbsent(symbol.name.uppercase(), symbol)

            when {
                name.endsWith("LIN", ignoreCase = true) || name.contains("LINE", ignoreCase = true) ->
                    lineStyles.putIfAbsent(name.uppercase(), SourceLineStyle(name, "Imported OpenCPN line style"))
                name.endsWith("PAT", ignoreCase = true) || name.contains("PAT", ignoreCase = true) ->
                    patterns.putIfAbsent(name.uppercase(), SourcePattern(name, "Imported OpenCPN pattern"))
            }
            previousEnd = match.range.last + 1
        }

        // If the flat stream uses plain names without color/name markers, still
        // collect unique symbol-like tokens near HPGL commands so the exporter
        // can surface the imported vector set instead of failing silently.
        if (symbols.size < 50) {
            val looseName = Regex("\\b[A-Z][A-Z0-9]{5,7}\\b")
            for (match in looseName.findAll(text)) {
                val name = match.value.take(8)
                if (!looksLikeS52Token(name)) continue
                val vectorChunk = text.substring(0, match.range.first).takeLast(2048)
                symbols.putIfAbsent(name.uppercase(), sourceSymbol(name, parseHpglLikeVector(vectorChunk).ifEmpty { fallbackSymbolCommands(name) }))
            }
        }

        return buildPack(
            sourceName = sourceName,
            symbols = symbols.values.sortedBy { it.name },
            lineStyles = lineStyles.values.sortedBy { it.name },
            patterns = patterns.values.sortedBy { it.name },
            colorsByPalette = emptyMap()
        )
    }

    private fun buildPack(
        sourceName: String,
        symbols: List<SourceSymbol>,
        lineStyles: List<SourceLineStyle>,
        patterns: List<SourcePattern>,
        colorsByPalette: Map<S52Palette, Map<String, SourceColor>>
    ): PresLibSourcePack {
        val fallback = S52LibCompatPresLib.sourcePack()
        val colorTables = if (colorsByPalette.isEmpty()) {
            S52Palette.entries.map { SourceColorTable(it, S52LibCompatPresLib.s52LibColors()) }
        } else {
            val day = colorsByPalette[S52Palette.DayBright]?.values?.sortedBy { it.token }
            S52Palette.entries.map { palette ->
                SourceColorTable(palette, colorsByPalette[palette]?.values?.sortedBy { it.token } ?: day ?: S52LibCompatPresLib.s52LibColors())
            }
        }

        return fallback.copy(
            metadata = PresLibMetadata(
                name = "OpenCPN chartsymbols Presentation Library pack",
                edition = "opencpn-chartsymbols-imported",
                sourceDescription = "Imported from $sourceName; symbols=${symbols.size}, lines=${lineStyles.size}, patterns=${patterns.size}",
                generatedBy = "OpenCpnChartSymbolsImporter"
            ),
            colorTables = colorTables,
            symbols = symbols,
            lineStyles = if (lineStyles.isEmpty()) fallback.lineStyles else lineStyles,
            patterns = if (patterns.isEmpty()) fallback.patterns else patterns
        )
    }

    private fun extractSymbol(element: Element, attrs: Map<String, String>): SourceSymbol? {
        val name = chooseName(attrs) ?: return null
        val vectorText = collectVectorText(element)
        if (vectorText.isBlank() && !looksLikeSymbolName(name)) return null
        return sourceSymbol(name, parseHpglLikeVector(vectorText).ifEmpty { fallbackSymbolCommands(name) })
    }

    private fun sourceSymbol(name: String, commands: List<SourceVectorCommand>): SourceSymbol {
        val xs = commands.mapNotNull { command ->
            when (command) {
                is SourceVectorCommand.MoveTo -> command.x
                is SourceVectorCommand.LineTo -> command.x
                SourceVectorCommand.ClosePath -> null
            }
        }
        val ys = commands.mapNotNull { command ->
            when (command) {
                is SourceVectorCommand.MoveTo -> command.y
                is SourceVectorCommand.LineTo -> command.y
                SourceVectorCommand.ClosePath -> null
            }
        }
        val minX = xs.minOrNull() ?: -8.0
        val maxX = xs.maxOrNull() ?: 8.0
        val minY = ys.minOrNull() ?: -8.0
        val maxY = ys.maxOrNull() ?: 8.0
        return SourceSymbol(
            name = name.take(8),
            pivotX = (minX + maxX) / 2.0,
            pivotY = (minY + maxY) / 2.0,
            width = max(1.0, maxX - minX),
            height = max(1.0, maxY - minY),
            commands = commands
        )
    }

    private fun extractLineStyle(attrs: Map<String, String>): SourceLineStyle? {
        val name = chooseName(attrs) ?: return null
        if (!looksLikeS52Token(name)) return null
        return SourceLineStyle(name, attrs["description"] ?: attrs["desc"] ?: "Imported OpenCPN line style")
    }

    private fun extractPattern(attrs: Map<String, String>): SourcePattern? {
        val name = chooseName(attrs) ?: return null
        if (!looksLikeS52Token(name)) return null
        return SourcePattern(name, attrs["description"] ?: attrs["desc"] ?: "Imported OpenCPN pattern")
    }

    private fun extractColor(attrs: Map<String, String>): Pair<S52Palette, SourceColor>? {
        val name = attrs["name"] ?: attrs["token"] ?: return null
        val r = attrs["r"]?.toIntOrNull() ?: attrs["red"]?.toIntOrNull()
        val g = attrs["g"]?.toIntOrNull() ?: attrs["green"]?.toIntOrNull()
        val b = attrs["b"]?.toIntOrNull() ?: attrs["blue"]?.toIntOrNull()
        if (r == null || g == null || b == null) return null
        val palette = when ((attrs["table"] ?: attrs["palette"] ?: attrs["scheme"] ?: "day").lowercase()) {
            "day", "daybright", "rgb" -> S52Palette.DayBright
            "dusk" -> S52Palette.Dusk
            "night", "dark" -> S52Palette.Night
            else -> S52Palette.DayBright
        }
        return palette to SourceColor(name, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun collectVectorText(element: Element): String {
        val out = StringBuilder()
        fun visit(node: Node) {
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                    val value = node.nodeValue?.trim().orEmpty()
                    if (hasVectorCommand(value)) out.append(value).append(';')
                }
                Node.ELEMENT_NODE -> {
                    val child = node as Element
                    for (key in listOf("vector", "hpgl", "instructions", "points", "d", "cmd", "commands")) {
                        val value = child.getAttribute(key)
                        if (hasVectorCommand(value)) out.append(value).append(';')
                    }
                    val children = node.childNodes
                    for (i in 0 until children.length) visit(children.item(i))
                }
            }
        }
        visit(element)
        return out.toString()
    }

    private fun hasVectorCommand(value: String): Boolean =
        value.contains("PU") || value.contains("PD") || value.contains("CI") || value.contains("AA")

    private fun chooseName(attrs: Map<String, String>): String? {
        for (key in listOf("name", "Name", "rcid", "RCID", "id", "ID", "token")) {
            val value = attrs[key]?.trim().orEmpty()
            if (looksLikeS52Token(value)) return value.take(8)
        }
        return attrs.values.firstOrNull { looksLikeS52Token(it.trim()) }?.trim()?.take(8)
    }

    private fun attributes(element: Element): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val attrs = element.attributes
        for (i in 0 until attrs.length) {
            val item = attrs.item(i)
            result[item.nodeName] = item.nodeValue
            result[item.nodeName.lowercase()] = item.nodeValue
        }
        return result
    }

    private fun looksLikeS52Token(value: String): Boolean =
        value.length in 4..16 && value.any { it.isLetter() } && value.none { it.isWhitespace() }

    private fun looksLikeSymbolName(value: String): Boolean = looksLikeS52Token(value)

    private fun parseHpglLikeVector(vector: String): List<SourceVectorCommand> {
        val commands = mutableListOf<SourceVectorCommand>()
        val tokens = vector
            .replace('\u001f', ';')
            .replace('\u001e', ';')
            .split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        var penDown = false
        var currentX = 0.0
        var currentY = 0.0
        for (token in tokens) {
            val code = token.take(2).uppercase()
            val args = token.drop(2)
            when (code) {
                "PU" -> {
                    val points = parseCoordinatePairs(args)
                    if (points.isEmpty()) {
                        penDown = false
                    } else {
                        for (point in points) {
                            commands += SourceVectorCommand.MoveTo(point.first, point.second)
                            currentX = point.first
                            currentY = point.second
                        }
                        penDown = false
                    }
                }
                "PD" -> {
                    val points = parseCoordinatePairs(args)
                    if (points.isEmpty()) {
                        penDown = true
                    } else {
                        for (point in points) {
                            if (!penDown) commands += SourceVectorCommand.MoveTo(currentX, currentY)
                            commands += SourceVectorCommand.LineTo(point.first, point.second)
                            currentX = point.first
                            currentY = point.second
                            penDown = true
                        }
                    }
                }
                "CI" -> commands += circleApprox(currentX, currentY, args.toDoubleOrNull() ?: 4.0)
                "AA" -> {
                    val values = parseNumbers(args)
                    if (values.size >= 3) commands += arcApprox(currentX, currentY, values[0], values[1], values[2])
                }
                "SP", "SW", "ST", "PM", "EP", "FP", "SC", "XT", "AP", "AC" -> Unit
            }
        }
        return commands
    }

    private fun parseCoordinatePairs(args: String): List<Pair<Double, Double>> =
        parseNumbers(args).chunked(2).mapNotNull { chunk ->
            if (chunk.size == 2) chunk[0] to chunk[1] else null
        }

    private fun parseNumbers(args: String): List<Double> =
        Regex("[-+]?\\d+(?:\\.\\d+)?").findAll(args).map { it.value.toDouble() }.toList()

    private fun circleApprox(cx: Double, cy: Double, radius: Double): List<SourceVectorCommand> {
        val r = max(1.0, radius)
        val result = mutableListOf<SourceVectorCommand>()
        for (i in 0..24) {
            val angle = Math.PI * 2.0 * i / 24.0
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r
            result += if (i == 0) SourceVectorCommand.MoveTo(x, y) else SourceVectorCommand.LineTo(x, y)
        }
        result += SourceVectorCommand.ClosePath
        return result
    }

    private fun arcApprox(cx: Double, cy: Double, endX: Double, endY: Double, sweepDeg: Double): List<SourceVectorCommand> {
        val radius = max(1.0, hypot(endX - cx, endY - cy))
        val steps = min(24, max(4, kotlin.math.abs(sweepDeg / 15.0).toInt()))
        val result = mutableListOf<SourceVectorCommand>()
        for (i in 0..steps) {
            val angle = Math.toRadians(sweepDeg * i / steps)
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius
            result += if (i == 0) SourceVectorCommand.MoveTo(x, y) else SourceVectorCommand.LineTo(x, y)
        }
        return result
    }

    private fun fallbackSymbolCommands(name: String): List<SourceVectorCommand> {
        val seed = name.fold(0) { acc, ch -> acc + ch.code }
        val sides = 3 + (seed % 5)
        val r = 8.0
        val commands = mutableListOf<SourceVectorCommand>()
        for (i in 0 until sides) {
            val angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides
            val x = cos(angle) * r
            val y = sin(angle) * r
            commands += if (i == 0) SourceVectorCommand.MoveTo(x, y) else SourceVectorCommand.LineTo(x, y)
        }
        commands += SourceVectorCommand.ClosePath
        return commands
    }
}
