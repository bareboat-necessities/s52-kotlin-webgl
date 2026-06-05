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
 * Phase 22 importer for OpenCPN `chartsymbols.xml`.
 *
 * This importer intentionally targets scalable/vector data only. It does not
 * read `rastersymbols-*.png` atlases.
 */
object OpenCpnChartSymbolsImporter {
    fun importFile(file: File): PresLibSourcePack {
        require(file.isFile) { "OpenCPN chartsymbols.xml file does not exist: ${file.absolutePath}" }
        return importXml(file.readText(), sourceName = file.name)
    }

    fun importXml(xml: String, sourceName: String = "chartsymbols.xml"): PresLibSourcePack {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            try { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } catch (_: Throwable) {}
        }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val all = doc.getElementsByTagName("*")

        val symbols = linkedMapOf<String, SourceSymbol>()
        val lineStyles = linkedMapOf<String, SourceLineStyle>()
        val patterns = linkedMapOf<String, SourcePattern>()
        val colorsByPalette = mutableMapOf<S52Palette, LinkedHashMap<String, SourceColor>>()

        for (i in 0 until all.length) {
            val element = all.item(i) as? Element ?: continue
            val tag = element.tagName.lowercase()
            val attrs = attributes(element)
            if (tag.contains("symbol") && !tag.contains("graphics-location") && !tag.contains("bitmap")) {
                extractSymbol(element, attrs)?.let { symbols.putIfAbsent(it.name.uppercase(), it) }
            }
            if (tag.contains("line") && (tag.contains("style") || tag.contains("symbol") || tag == "line")) {
                extractLineStyle(element, attrs)?.let { lineStyles.putIfAbsent(it.name.uppercase(), it) }
            }
            if (tag.contains("pattern") || tag.contains("fill")) {
                extractPattern(element, attrs)?.let { patterns.putIfAbsent(it.name.uppercase(), it) }
            }
            if (tag.contains("color")) {
                extractColor(element, attrs)?.let { (palette, color) ->
                    colorsByPalette.getOrPut(palette) { linkedMapOf() }.putIfAbsent(color.token.uppercase(), color)
                }
            }
        }
        val colors = if (colorsByPalette.isEmpty()) {
            S52Palette.entries.map { SourceColorTable(it, S52LibCompatPresLib.s52LibColors()) }
        } else {
            S52Palette.entries.map { palette ->
                val table = colorsByPalette[palette]?.values?.sortedBy { it.token }
                    ?: colorsByPalette[S52Palette.DayBright]?.values?.sortedBy { it.token }
                    ?: S52LibCompatPresLib.s52LibColors()
                SourceColorTable(palette, table)
            }
        }

        return S52LibCompatPresLib.sourcePack().copy(
            metadata = PresLibMetadata(
                name = "OpenCPN chartsymbols Presentation Library pack",
                edition = "opencpn-chartsymbols-imported",
                sourceDescription = "Imported from $sourceName; symbols=${symbols.size}, lines=${lineStyles.size}, patterns=${patterns.size}",
                generatedBy = "OpenCpnChartSymbolsImporter"
            ),
            colorTables = colors,
            symbols = symbols.values.sortedBy { it.name },
            lineStyles = if (lineStyles.isEmpty()) S52LibCompatPresLib.sourcePack().lineStyles else lineStyles.values.sortedBy { it.name },
            patterns = if (patterns.isEmpty()) S52LibCompatPresLib.sourcePack().patterns else patterns.values.sortedBy { it.name }
        )
    }

    private fun extractSymbol(element: Element, attrs: Map<String, String>): SourceSymbol? {
        val name = chooseName(attrs) ?: return null
        val vectorText = collectVectorText(element)
        if (vectorText.isBlank() && !looksLikeSymbolName(name)) return null
        val commands = parseHpglLikeVector(vectorText).ifEmpty { fallbackSymbolCommands(name) }
        val xs = commands.mapNotNull { when (it) { is SourceVectorCommand.MoveTo -> it.x; is SourceVectorCommand.LineTo -> it.x; SourceVectorCommand.ClosePath -> null } }
        val ys = commands.mapNotNull { when (it) { is SourceVectorCommand.MoveTo -> it.y; is SourceVectorCommand.LineTo -> it.y; SourceVectorCommand.ClosePath -> null } }
        val minX = xs.minOrNull() ?: -8.0
        val maxX = xs.maxOrNull() ?: 8.0
        val minY = ys.minOrNull() ?: -8.0
        val maxY = ys.maxOrNull() ?: 8.0
        return SourceSymbol(
            name = name,
            pivotX = (minX + maxX) / 2.0,
            pivotY = (minY + maxY) / 2.0,
            width = max(1.0, maxX - minX),
            height = max(1.0, maxY - minY),
            commands = commands
        )
    }

    private fun extractLineStyle(element: Element, attrs: Map<String, String>): SourceLineStyle? {
        val name = chooseName(attrs) ?: return null
        if (!looksLikeS52Token(name)) return null
        return SourceLineStyle(name, attrs["description"] ?: attrs["desc"] ?: "Imported OpenCPN line style")
    }

    private fun extractPattern(element: Element, attrs: Map<String, String>): SourcePattern? {
        val name = chooseName(attrs) ?: return null
        if (!looksLikeS52Token(name)) return null
        return SourcePattern(name, attrs["description"] ?: attrs["desc"] ?: "Imported OpenCPN pattern")
    }

    private fun extractColor(element: Element, attrs: Map<String, String>): Pair<S52Palette, SourceColor>? {
        val name = attrs["name"] ?: attrs["token"] ?: return null
        val r = attrs["r"]?.toIntOrNull() ?: attrs["red"]?.toIntOrNull()
        val g = attrs["g"]?.toIntOrNull() ?: attrs["green"]?.toIntOrNull()
        val b = attrs["b"]?.toIntOrNull() ?: attrs["blue"]?.toIntOrNull()
        if (r == null || g == null || b == null) return null
        val palette = when ((attrs["table"] ?: attrs["palette"] ?: attrs["scheme"] ?: "day").lowercase()) {
            "day", "daybright", "rgb", "DAY".lowercase() -> S52Palette.DayBright
            "dusk" -> S52Palette.Dusk
            "night", "dark" -> S52Palette.Night
            else -> S52Palette.DayBright
        }
        return palette to SourceColor(name, r.coerceIn(0,255), g.coerceIn(0,255), b.coerceIn(0,255))
    }

    private fun collectVectorText(element: Element): String {
        val out = StringBuilder()
        fun visit(node: Node) {
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                    val text = node.nodeValue?.trim().orEmpty()
                    if (text.contains("PU") || text.contains("PD") || text.contains("CI") || text.contains("AA")) {
                        out.append(text).append(';')
                    }
                }
                Node.ELEMENT_NODE -> {
                    val el = node as Element
                    for (k in listOf("vector", "hpgl", "instructions", "points", "d", "cmd", "commands")) {
                        el.getAttribute(k).takeIf { it.isNotBlank() }?.let {
                            if (it.contains("PU") || it.contains("PD") || it.contains("CI") || it.contains("AA")) {
                                out.append(it).append(';')
                            }
                        }
                    }
                    val childNodes = node.childNodes
                    for (i in 0 until childNodes.length) visit(childNodes.item(i))
                }
            }
        }
        visit(element)
        return out.toString()
    }

    private fun chooseName(attrs: Map<String, String>): String? {
        val preferred = listOf("name", "Name", "rcid", "RCID", "id", "ID", "token", "description")
        for (key in preferred) {
            val value = attrs[key]?.trim().orEmpty()
            if (looksLikeS52Token(value)) return value.take(8)
        }
        return attrs.values.firstOrNull { looksLikeS52Token(it) }?.trim()?.take(8)
    }

    private fun attributes(element: Element): Map<String, String> = buildMap {
        val attrs = element.attributes
        for (i in 0 until attrs.length) {
            val item = attrs.item(i)
            put(item.nodeName, item.nodeValue)
            put(item.nodeName.lowercase(), item.nodeValue)
        }
    }

    private fun looksLikeS52Token(value: String): Boolean = value.length in 4..16 && value.any(Char::isLetter) && value.none { it == ' ' || it == '
' || it == '
' }
    private fun looksLikeSymbolName(value: String): Boolean = looksLikeS52Token(value)

    private fun parseHpglLikeVector(vector: String): List<SourceVectorCommand> {
        val commands = mutableListOf<SourceVectorCommand>()
        val tokens = vector.split(';', '', '').map { it.trim() }.filter { it.isNotEmpty() }
        var penDown = false
        var currentX = 0.0
        var currentY = 0.0
        for (token in tokens) {
            val code = token.take(2).uppercase()
            val args = token.drop(2)
            when (code) {
                "PU" -> {
                    val pts = parseCoordinatePairs(args)
                    if (pts.isEmpty()) penDown = false else {
                        for ((x, y) in pts) {
                            commands += SourceVectorCommand.MoveTo(x, y)
                            currentX = x; currentY = y
                        }
                        penDown = false
                    }
                }
                "PD" -> {
                    val pts = parseCoordinatePairs(args)
                    if (pts.isEmpty()) penDown = true else {
                        for ((x, y) in pts) {
                            if (!penDown) commands += SourceVectorCommand.MoveTo(currentX, currentY)
                            commands += SourceVectorCommand.LineTo(x, y)
                            currentX = x; currentY = y; penDown = true
                        }
                    }
                }
                "CI" -> commands += circleApprox(currentX, currentY, args.toDoubleOrNull() ?: 4.0)
                "AA" -> {
                    val nums = parseNumbers(args)
                    if (nums.size >= 3) commands += arcApprox(currentX, currentY, nums[0], nums[1], nums[2])
                }
                "SP", "SW", "ST", "PM", "EP", "FP", "SC", "XT", "AP", "AC" -> Unit
            }
        }
        return commands
    }

    private fun parseCoordinatePairs(args: String): List<Pair<Double, Double>> = parseNumbers(args).chunked(2).mapNotNull { if (it.size == 2) it[0] to it[1] else null }
    private fun parseNumbers(args: String): List<Double> = Regex("[-+]?\d+(?:\.\d+)?").findAll(args).map { it.value.toDouble() }.toList()

    private fun circleApprox(cx: Double, cy: Double, radius: Double): List<SourceVectorCommand> {
        val r = max(1.0, radius)
        val result = mutableListOf<SourceVectorCommand>()
        for (i in 0..24) {
            val a = Math.PI * 2.0 * i / 24.0
            val x = cx + cos(a) * r
            val y = cy + sin(a) * r
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
            val a = Math.toRadians(sweepDeg * i / steps)
            val x = cx + cos(a) * radius
            val y = cy + sin(a) * radius
            result += if (i == 0) SourceVectorCommand.MoveTo(x, y) else SourceVectorCommand.LineTo(x, y)
        }
        return result
    }

    private fun fallbackSymbolCommands(name: String): List<SourceVectorCommand> {
        val seed = name.fold(0) { acc, ch -> acc + ch.code }
        val sides = 3 + (seed % 5)
        val r = 8.0
        val cmds = mutableListOf<SourceVectorCommand>()
        for (i in 0 until sides) {
            val a = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides
            val x = cos(a) * r
            val y = sin(a) * r
            cmds += if (i == 0) SourceVectorCommand.MoveTo(x, y) else SourceVectorCommand.LineTo(x, y)
        }
        cmds += SourceVectorCommand.ClosePath
        return cmds
    }
}
