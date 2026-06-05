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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase 25 importer for OpenCPN `chartsymbols.xml`.
 *
 * The file is XML. OpenCPN reads symbol, line-style, pattern, color-table,
 * vector, pivot/origin, and HPGL nodes from it. This importer mirrors that
 * structure and intentionally ignores raster atlas data.
 */
object OpenCpnChartSymbolsImporter {
    private const val ImportedEdition = "opencpn-chartsymbols-imported"

    fun importFile(file: File): PresLibSourcePack {
        require(file.isFile) { "OpenCPN chartsymbols.xml file does not exist: ${file.absolutePath}" }
        return importXml(file.readText(), sourceName = file.name)
    }

    fun importXml(xml: String, sourceName: String = "chartsymbols.xml"): PresLibSourcePack {
        val document = documentBuilder().parse(xml.byteInputStream())

        val symbols = parseSymbols(document.documentElement)
        val lineStyles = parseLineStyles(document.documentElement)
        val patterns = parsePatterns(document.documentElement)
        val colorsByPalette = parseColorTables(document.documentElement)

        return buildPack(
            sourceName = sourceName,
            symbols = symbols,
            lineStyles = lineStyles,
            patterns = patterns,
            colorsByPalette = colorsByPalette,
            sourceDescription = "Imported from OpenCPN XML chartsymbols.xml; symbols=${symbols.size}, lines=${lineStyles.size}, patterns=${patterns.size}"
        )
    }

    fun importText(text: String, sourceName: String = "chartsymbols.xml"): PresLibSourcePack =
        importXml(text, sourceName)

    private fun documentBuilder() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isExpandEntityReferences = false
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    }.newDocumentBuilder()

    private fun parseColorTables(root: Element): Map<S52Palette, LinkedHashMap<String, SourceColor>> {
        val result = mutableMapOf<S52Palette, LinkedHashMap<String, SourceColor>>()
        val colorTables = root.getElementsByTagName("color-table")
        for (i in 0 until colorTables.length) {
            val table = colorTables.item(i) as? Element ?: continue
            val palette = paletteFrom(table.getAttribute("name").ifBlank { table.getAttribute("table-name") })
            val colors = result.getOrPut(palette) { linkedMapOf() }
            val colorNodes = table.getElementsByTagName("color")
            for (j in 0 until colorNodes.length) {
                val color = colorNodes.item(j) as? Element ?: continue
                val name = color.getAttribute("name").takeIf { it.isNotBlank() } ?: continue
                val r = color.getAttribute("r").toIntOrNull() ?: continue
                val g = color.getAttribute("g").toIntOrNull() ?: continue
                val b = color.getAttribute("b").toIntOrNull() ?: continue
                colors[name.uppercase()] = SourceColor(name.take(8), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
        return result
    }

    private fun parseLineStyles(root: Element): List<SourceLineStyle> {
        val result = linkedMapOf<String, SourceLineStyle>()
        val containers = elementsNamed(root, "line-styles", "lineStyles", "linestyles")
        for (container in containers) {
            for (child in childElements(container)) {
                val name = directText(child, "name") ?: child.getAttribute("name").takeIf { it.isNotBlank() } ?: continue
                val hpgl = directText(child, "HPGL")
                if (!looksLikeS52Token(name) || hpgl.isNullOrBlank()) continue
                val description = directText(child, "description") ?: "Imported OpenCPN line style"
                result.putIfAbsent(name.uppercase(), SourceLineStyle(name.take(8), description))
            }
        }
        return result.values.sortedBy { it.name }
    }

    private fun parsePatterns(root: Element): List<SourcePattern> {
        val result = linkedMapOf<String, SourcePattern>()
        val containers = elementsNamed(root, "patterns")
        for (container in containers) {
            for (child in childElements(container)) {
                val name = directText(child, "name") ?: child.getAttribute("name").takeIf { it.isNotBlank() } ?: continue
                val hpgl = directText(child, "HPGL")
                val definition = directText(child, "definition")
                if (!looksLikeS52Token(name) || (hpgl.isNullOrBlank() && definition != "V")) continue
                val description = directText(child, "description") ?: "Imported OpenCPN pattern"
                result.putIfAbsent(name.uppercase(), SourcePattern(name.take(8), description))
            }
        }
        return result.values.sortedBy { it.name }
    }

    private fun parseSymbols(root: Element): List<SourceSymbol> {
        val result = linkedMapOf<String, SourceSymbol>()
        val containers = elementsNamed(root, "symbols")
        for (container in containers) {
            for (symbolElement in childElements(container)) {
                val name = directText(symbolElement, "name") ?: symbolElement.getAttribute("name").takeIf { it.isNotBlank() } ?: continue
                if (!looksLikeS52Token(name)) continue

                val vector = directChild(symbolElement, "vector") ?: continue
                val hpgl = directText(vector, "HPGL") ?: continue
                val commands = parseHpglLikeVector(hpgl)
                if (commands.isEmpty()) continue

                val size = parseVectorSize(vector)
                result.putIfAbsent(
                    name.uppercase(),
                    symbolFromCommands(
                        name = name.take(8),
                        commands = commands,
                        pivotX = size.pivotX,
                        pivotY = size.pivotY,
                        explicitWidth = size.width,
                        explicitHeight = size.height
                    )
                )
            }
        }
        return result.values.sortedBy { it.name }
    }

    private data class VectorSize(
        val width: Double?,
        val height: Double?,
        val pivotX: Double?,
        val pivotY: Double?
    )

    private fun parseVectorSize(vector: Element): VectorSize {
        val width = vector.getAttribute("width").toDoubleOrNull()
        val height = vector.getAttribute("height").toDoubleOrNull()
        val pivot = directChild(vector, "pivot")
        return VectorSize(
            width = width,
            height = height,
            pivotX = pivot?.getAttribute("x")?.toDoubleOrNull(),
            pivotY = pivot?.getAttribute("y")?.toDoubleOrNull()
        )
    }

    private fun buildPack(
        sourceName: String,
        symbols: List<SourceSymbol>,
        lineStyles: List<SourceLineStyle>,
        patterns: List<SourcePattern>,
        colorsByPalette: Map<S52Palette, LinkedHashMap<String, SourceColor>>,
        sourceDescription: String
    ): PresLibSourcePack {
        val fallback = S52LibCompatPresLib.sourcePack()
        val colorTables = if (colorsByPalette.isEmpty()) {
            S52Palette.entries.map { SourceColorTable(it, S52LibCompatPresLib.s52LibColors()) }
        } else {
            S52Palette.entries.map { palette ->
                val colors = colorsByPalette[palette]?.values?.sortedBy { it.token }
                    ?: colorsByPalette[S52Palette.DayBright]?.values?.sortedBy { it.token }
                    ?: S52LibCompatPresLib.s52LibColors()
                SourceColorTable(palette, colors)
            }
        }

        return fallback.copy(
            metadata = PresLibMetadata(
                name = "OpenCPN chartsymbols Presentation Library pack",
                edition = ImportedEdition,
                sourceDescription = "$sourceDescription; source=$sourceName",
                generatedBy = "OpenCpnChartSymbolsImporter"
            ),
            colorTables = colorTables,
            symbols = symbols,
            lineStyles = if (lineStyles.isEmpty()) fallback.lineStyles else lineStyles,
            patterns = if (patterns.isEmpty()) fallback.patterns else patterns
        )
    }

    private fun symbolFromCommands(
        name: String,
        commands: List<SourceVectorCommand>,
        pivotX: Double?,
        pivotY: Double?,
        explicitWidth: Double?,
        explicitHeight: Double?
    ): SourceSymbol {
        val xs = commands.mapNotNull {
            when (it) {
                is SourceVectorCommand.MoveTo -> it.x
                is SourceVectorCommand.LineTo -> it.x
                SourceVectorCommand.ClosePath -> null
            }
        }
        val ys = commands.mapNotNull {
            when (it) {
                is SourceVectorCommand.MoveTo -> it.y
                is SourceVectorCommand.LineTo -> it.y
                SourceVectorCommand.ClosePath -> null
            }
        }
        val minX = xs.minOrNull() ?: 0.0
        val maxX = xs.maxOrNull() ?: 1.0
        val minY = ys.minOrNull() ?: 0.0
        val maxY = ys.maxOrNull() ?: 1.0
        return SourceSymbol(
            name = name,
            pivotX = pivotX ?: ((minX + maxX) / 2.0),
            pivotY = pivotY ?: ((minY + maxY) / 2.0),
            width = max(1.0, explicitWidth ?: (maxX - minX)),
            height = max(1.0, explicitHeight ?: (maxY - minY)),
            commands = commands
        )
    }

    private fun parseHpglLikeVector(hpgl: String): List<SourceVectorCommand> {
        val commands = mutableListOf<SourceVectorCommand>()
        val tokenRegex = Regex("(PU|PD|CI|AA|SP|SW|ST|PM|EP|FP|SC|AP|AC)([^;]*)")
        var penDown = false
        var currentX = 0.0
        var currentY = 0.0

        for (match in tokenRegex.findAll(hpgl)) {
            val code = match.groupValues[1]
            val args = match.groupValues[2]
            when (code) {
                "PU" -> {
                    val points = parseCoordinatePairs(args)
                    if (points.isEmpty()) {
                        penDown = false
                    } else {
                        for (point in points) {
                            currentX = point.first
                            currentY = point.second
                            commands += SourceVectorCommand.MoveTo(currentX, currentY)
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
                            currentX = point.first
                            currentY = point.second
                            commands += SourceVectorCommand.LineTo(currentX, currentY)
                            penDown = true
                        }
                    }
                }
                "CI" -> commands += circleApprox(currentX, currentY, args.trim().toDoubleOrNull() ?: 4.0)
                "AA" -> {
                    val nums = parseNumbers(args)
                    if (nums.size >= 3) commands += arcApprox(currentX, currentY, nums[0], nums[1], nums[2])
                }
            }
        }
        return commands
    }

    private fun parseCoordinatePairs(args: String): List<Pair<Double, Double>> {
        val nums = parseNumbers(args)
        val result = mutableListOf<Pair<Double, Double>>()
        var index = 0
        while (index + 1 < nums.size) {
            result += nums[index] to nums[index + 1]
            index += 2
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

    private fun arcApprox(cx: Double, cy: Double, endX: Double, endY: Double, sweepDeg: Double): List<SourceVectorCommand> {
        val radius = max(1.0, hypot(endX - cx, endY - cy))
        val steps = min(24, max(4, abs(sweepDeg / 15.0).toInt()))
        val result = mutableListOf<SourceVectorCommand>()
        for (i in 0..steps) {
            val angle = Math.toRadians(sweepDeg * i / steps)
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius
            result += if (i == 0) SourceVectorCommand.MoveTo(x, y) else SourceVectorCommand.LineTo(x, y)
        }
        return result
    }

    private fun elementsNamed(root: Element, vararg names: String): List<Element> {
        val result = mutableListOf<Element>()
        for (name in names) {
            val nodes = root.getElementsByTagName(name)
            for (i in 0 until nodes.length) {
                (nodes.item(i) as? Element)?.let { result += it }
            }
        }
        return result.distinct()
    }

    private fun childElements(element: Element): List<Element> {
        val result = mutableListOf<Element>()
        val children = element.childNodes
        for (i in 0 until children.length) {
            (children.item(i) as? Element)?.let { result += it }
        }
        return result
    }

    private fun directChild(element: Element, name: String): Element? =
        childElements(element).firstOrNull { it.tagName == name }

    private fun directText(element: Element, name: String): String? =
        directChild(element, name)?.textContent?.trim()?.takeIf { it.isNotBlank() }

    private fun paletteFrom(name: String): S52Palette = when (name.lowercase()) {
        "dusk" -> S52Palette.Dusk
        "night", "dark" -> S52Palette.Night
        "dayblackback" -> S52Palette.DayBlackBack
        "daywhiteback" -> S52Palette.DayWhiteBack
        else -> S52Palette.DayBright
    }

    private fun looksLikeS52Token(value: String): Boolean =
        value.length in 4..16 && value.any(Char::isLetter) && value.none { it.isWhitespace() }
}
