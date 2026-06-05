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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase 26 OpenCPN chartsymbols.xml importer.
 *
 * OpenCPN's parser reads color-table/color RGB rows, line-style HPGL rows,
 * pattern HPGL rows, and symbol HPGL nested below the vector node. This importer
 * follows that XML structure and also keeps a rich, raw-HPGL view for SVG export
 * so color changes and contour/fill commands are not lost.
 */
object OpenCpnChartSymbolsImporter {
    private const val ImportedEdition = "opencpn-chartsymbols-imported"

    fun importFile(file: File): PresLibSourcePack = importRenderableFile(file).sourcePack

    fun importXml(xml: String, sourceName: String = "chartsymbols.xml"): PresLibSourcePack =
        importRenderableXml(xml, sourceName).sourcePack

    fun importRenderableFile(file: File): OpenCpnRenderablePack {
        require(file.isFile) { "OpenCPN chartsymbols.xml file does not exist: ${file.absolutePath}" }
        return importRenderableXml(file.readText(), file.name)
    }

    fun importRenderableXml(xml: String, sourceName: String = "chartsymbols.xml"): OpenCpnRenderablePack {
        val document = parseXml(xml)
        val root = document.documentElement
        val colorsByPalette = parseColorTables(root)
        val symbols = parseSymbols(root)
        val lineStyles = parseLineStyles(root)
        val patterns = parsePatterns(root)

        val sourcePack = buildSourcePack(
            sourceName = sourceName,
            symbols = symbols,
            lineStyles = lineStyles,
            patterns = patterns,
            colorsByPalette = colorsByPalette
        )

        return OpenCpnRenderablePack(
            sourcePack = sourcePack,
            symbols = symbols,
            lineStyles = lineStyles,
            patterns = patterns,
            colorsByPalette = colorsByPalette
        )
    }

    private fun parseXml(xml: String): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        return factory.newDocumentBuilder().parse(xml.byteInputStream())
    }

    private fun parseColorTables(root: Element): Map<S52Palette, List<SourceColor>> {
        val tables = linkedMapOf<S52Palette, MutableList<SourceColor>>()
        val colorTableNodes = root.getElementsByTagName("color-table")
        for (i in 0 until colorTableNodes.length) {
            val table = colorTableNodes.item(i) as? Element ?: continue
            val palette = paletteFromName(table.getAttribute("name").ifBlank { table.getAttribute("palette") })
            val colors = tables.getOrPut(palette) { mutableListOf() }
            table.childElements("color").forEach { colorNode ->
                val name = colorNode.getAttribute("name").trim().takeIf { it.isNotBlank() } ?: return@forEach
                val r = colorNode.getAttribute("r").toIntOrNull() ?: return@forEach
                val g = colorNode.getAttribute("g").toIntOrNull() ?: return@forEach
                val b = colorNode.getAttribute("b").toIntOrNull() ?: return@forEach
                colors += SourceColor(name.take(8), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
        return tables.mapValues { (_, colors) -> colors.distinctBy { it.token.uppercase() }.sortedBy { it.token } }
    }

    private fun parseLineStyles(root: Element): List<OpenCpnRenderableAsset> =
        root.getElementsByTagName("line-style").asElements().mapNotNull { line ->
            val name = line.childText("name") ?: line.getAttribute("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val vector = line.childElement("vector")
            val hpgl = line.childText("HPGL").orEmpty()
            OpenCpnRenderableAsset(
                name = name.take(8),
                kind = OpenCpnAssetKind.LineStyle,
                description = line.childText("description").orEmpty(),
                colorRefs = parseColorRefs(line.childText("color-ref")),
                hpgl = hpgl,
                width = vector?.getAttribute("width")?.toDoubleOrNull() ?: 64.0,
                height = vector?.getAttribute("height")?.toDoubleOrNull() ?: 16.0,
                pivotX = vector?.childElement("pivot")?.getAttribute("x")?.toDoubleOrNull() ?: 0.0,
                pivotY = vector?.childElement("pivot")?.getAttribute("y")?.toDoubleOrNull() ?: 0.0,
                commands = parseHpglAsSourceCommands(hpgl)
            )
        }

    private fun parsePatterns(root: Element): List<OpenCpnRenderableAsset> =
        root.getElementsByTagName("pattern").asElements().mapNotNull { pattern ->
            val name = pattern.childText("name") ?: pattern.getAttribute("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val vector = pattern.childElement("vector")
            val hpgl = pattern.childText("HPGL").orEmpty()
            OpenCpnRenderableAsset(
                name = name.take(8),
                kind = OpenCpnAssetKind.Pattern,
                description = pattern.childText("description").orEmpty(),
                colorRefs = parseColorRefs(pattern.childText("color-ref")),
                hpgl = hpgl,
                width = vector?.getAttribute("width")?.toDoubleOrNull() ?: 32.0,
                height = vector?.getAttribute("height")?.toDoubleOrNull() ?: 32.0,
                pivotX = vector?.childElement("pivot")?.getAttribute("x")?.toDoubleOrNull() ?: 0.0,
                pivotY = vector?.childElement("pivot")?.getAttribute("y")?.toDoubleOrNull() ?: 0.0,
                commands = parseHpglAsSourceCommands(hpgl)
            )
        }

    private fun parseSymbols(root: Element): List<OpenCpnRenderableAsset> =
        root.getElementsByTagName("symbol").asElements().mapNotNull { symbol ->
            val name = symbol.childText("name") ?: symbol.getAttribute("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val vector = symbol.childElement("vector")
            val hpgl = vector?.childText("HPGL") ?: symbol.childText("HPGL").orEmpty()
            val commands = parseHpglAsSourceCommands(hpgl).ifEmpty { fallbackSymbolCommands(name) }
            val bounds = bounds(commands)
            OpenCpnRenderableAsset(
                name = name.take(8),
                kind = OpenCpnAssetKind.Symbol,
                description = symbol.childText("description").orEmpty(),
                colorRefs = parseColorRefs(symbol.childText("color-ref")),
                hpgl = hpgl,
                width = vector?.getAttribute("width")?.toDoubleOrNull() ?: max(1.0, bounds.maxX - bounds.minX),
                height = vector?.getAttribute("height")?.toDoubleOrNull() ?: max(1.0, bounds.maxY - bounds.minY),
                pivotX = vector?.childElement("pivot")?.getAttribute("x")?.toDoubleOrNull() ?: (bounds.minX + bounds.maxX) / 2.0,
                pivotY = vector?.childElement("pivot")?.getAttribute("y")?.toDoubleOrNull() ?: (bounds.minY + bounds.maxY) / 2.0,
                commands = commands
            )
        }

    private fun buildSourcePack(
        sourceName: String,
        symbols: List<OpenCpnRenderableAsset>,
        lineStyles: List<OpenCpnRenderableAsset>,
        patterns: List<OpenCpnRenderableAsset>,
        colorsByPalette: Map<S52Palette, List<SourceColor>>
    ): PresLibSourcePack {
        val fallback = S52LibCompatPresLib.sourcePack()
        val colorTables = if (colorsByPalette.isEmpty()) {
            S52Palette.entries.map { SourceColorTable(it, S52LibCompatPresLib.s52LibColors()) }
        } else {
            S52Palette.entries.map { palette ->
                val colors = colorsByPalette[palette]
                    ?: colorsByPalette[S52Palette.DayBright]
                    ?: S52LibCompatPresLib.s52LibColors()
                SourceColorTable(palette, colors)
            }
        }
        return fallback.copy(
            metadata = PresLibMetadata(
                name = "OpenCPN chartsymbols Presentation Library pack",
                edition = ImportedEdition,
                sourceDescription = "Imported scalable/vector OpenCPN chartsymbols.xml from $sourceName; symbols=${symbols.size}, lines=${lineStyles.size}, patterns=${patterns.size}",
                generatedBy = "OpenCpnChartSymbolsImporter"
            ),
            colorTables = colorTables,
            symbols = symbols.map { it.toSourceSymbol() }.sortedBy { it.name },
            lineStyles = if (lineStyles.isEmpty()) fallback.lineStyles else lineStyles.map { SourceLineStyle(it.name, it.description) }.sortedBy { it.name },
            patterns = if (patterns.isEmpty()) fallback.patterns else patterns.map { SourcePattern(it.name, it.description) }.sortedBy { it.name }
        )
    }

    private fun OpenCpnRenderableAsset.toSourceSymbol(): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = pivotX,
        pivotY = pivotY,
        width = width,
        height = height,
        commands = commands
    )

    private fun parseColorRefs(text: String?): List<String> = text.orEmpty()
        .split(',', ';', ' ', '\t', '\n', '\r')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.take(8) }

    private fun paletteFromName(raw: String): S52Palette = when (raw.lowercase()) {
        "day", "daybright", "day-bright", "rgb" -> S52Palette.DayBright
        "dayblackback", "day-black-back" -> S52Palette.DayBlackBack
        "daywhiteback", "day-white-back" -> S52Palette.DayWhiteBack
        "dusk" -> S52Palette.Dusk
        "night", "dark" -> S52Palette.Night
        else -> S52Palette.DayBright
    }

    internal fun parseHpglAsSourceCommands(hpgl: String): List<SourceVectorCommand> {
        val commands = mutableListOf<SourceVectorCommand>()
        var currentX = 0.0
        var currentY = 0.0
        var penDown = false

        for (token in tokenizeHpgl(hpgl)) {
            when (token.code) {
                "PU" -> {
                    val pts = parseCoordinatePairs(token.args)
                    if (pts.isEmpty()) {
                        penDown = false
                    } else {
                        pts.forEach { point ->
                            currentX = point.first
                            currentY = point.second
                            commands += SourceVectorCommand.MoveTo(currentX, currentY)
                        }
                        penDown = false
                    }
                }
                "PD" -> {
                    val pts = parseCoordinatePairs(token.args)
                    if (pts.isEmpty()) {
                        penDown = true
                    } else {
                        pts.forEach { point ->
                            if (!penDown) commands += SourceVectorCommand.MoveTo(currentX, currentY)
                            currentX = point.first
                            currentY = point.second
                            commands += SourceVectorCommand.LineTo(currentX, currentY)
                            penDown = true
                        }
                    }
                }
                "CI" -> commands += circleApprox(currentX, currentY, token.args.toDoubleOrNull() ?: 4.0)
                "AA" -> {
                    val nums = parseNumbers(token.args)
                    if (nums.size >= 3) {
                        val arc = arcApprox(currentX, currentY, nums[0], nums[1], nums[2])
                        commands += arc
                        arc.lastOrNull()?.let { last ->
                            when (last) {
                                is SourceVectorCommand.MoveTo -> { currentX = last.x; currentY = last.y }
                                is SourceVectorCommand.LineTo -> { currentX = last.x; currentY = last.y }
                                SourceVectorCommand.ClosePath -> Unit
                            }
                        }
                    }
                }
            }
        }
        return commands
    }

    internal fun tokenizeHpgl(hpgl: String): List<HpglToken> {
        val tokens = mutableListOf<HpglToken>()
        var index = 0
        while (index < hpgl.length - 1) {
            val c0 = hpgl[index]
            val c1 = hpgl[index + 1]
            if (c0.isLetter() && c1.isLetter()) {
                val code = "${c0.uppercaseChar()}${c1.uppercaseChar()}"
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

    private fun arcApprox(startX: Double, startY: Double, centerX: Double, centerY: Double, sweepDeg: Double): List<SourceVectorCommand> {
        val radius = max(1.0, hypot(startX - centerX, startY - centerY))
        val startAngle = atan2(startY - centerY, startX - centerX)
        val sweep = Math.toRadians(sweepDeg)
        val steps = min(48, max(4, abs(sweepDeg / 10.0).toInt()))
        val result = mutableListOf<SourceVectorCommand>()
        for (i in 1..steps) {
            val angle = startAngle + sweep * i / steps
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius
            result += SourceVectorCommand.LineTo(x, y)
        }
        return result
    }

    private fun fallbackSymbolCommands(name: String): List<SourceVectorCommand> {
        val seed = name.fold(0) { acc, ch -> acc + ch.code }
        val sides = 3 + (seed % 5)
        val r = 8.0
        val result = mutableListOf<SourceVectorCommand>()
        for (i in 0 until sides) {
            val angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides
            val x = cos(angle) * r
            val y = sin(angle) * r
            result += if (i == 0) SourceVectorCommand.MoveTo(x, y) else SourceVectorCommand.LineTo(x, y)
        }
        result += SourceVectorCommand.ClosePath
        return result
    }

    private fun bounds(commands: List<SourceVectorCommand>): Bounds {
        val xs = commands.mapNotNull { when (it) { is SourceVectorCommand.MoveTo -> it.x; is SourceVectorCommand.LineTo -> it.x; SourceVectorCommand.ClosePath -> null } }
        val ys = commands.mapNotNull { when (it) { is SourceVectorCommand.MoveTo -> it.y; is SourceVectorCommand.LineTo -> it.y; SourceVectorCommand.ClosePath -> null } }
        return Bounds(xs.minOrNull() ?: -8.0, ys.minOrNull() ?: -8.0, xs.maxOrNull() ?: 8.0, ys.maxOrNull() ?: 8.0)
    }

    private fun Element.childText(name: String): String? = childElement(name)?.textContent?.trim()?.takeIf { it.isNotBlank() }

    private fun Element.childElement(name: String): Element? = childElements(name).firstOrNull()

    private fun Element.childElements(name: String): List<Element> = childNodes.asElements().filter { it.tagName == name }

    private fun org.w3c.dom.NodeList.asElements(): List<Element> {
        val result = mutableListOf<Element>()
        for (i in 0 until length) (item(i) as? Element)?.let(result::add)
        return result
    }

    data class HpglToken(val code: String, val args: String)
    private data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)
}

data class OpenCpnRenderablePack(
    val sourcePack: PresLibSourcePack,
    val symbols: List<OpenCpnRenderableAsset>,
    val lineStyles: List<OpenCpnRenderableAsset>,
    val patterns: List<OpenCpnRenderableAsset>,
    val colorsByPalette: Map<S52Palette, List<SourceColor>>
)

data class OpenCpnRenderableAsset(
    val name: String,
    val kind: OpenCpnAssetKind,
    val description: String,
    val colorRefs: List<String>,
    val hpgl: String,
    val width: Double,
    val height: Double,
    val pivotX: Double,
    val pivotY: Double,
    val commands: List<SourceVectorCommand>
)

enum class OpenCpnAssetKind {
    Symbol,
    LineStyle,
    Pattern
}
