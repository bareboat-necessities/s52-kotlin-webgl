package io.github.s52.preslib.opencpn.inventory

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Raw OpenCPN chartsymbols.xml parser for inventory and later code generation phases. */
object OpenCpnChartSymbolsRawParser {
    fun parseFile(file: File): OpenCpnChartSymbolsSummary {
        require(file.isFile) { "OpenCPN chartsymbols.xml does not exist: ${file.absolutePath}" }
        return parseXml(file.readText(), file.name)
    }

    fun parseXml(xml: String, sourceName: String = "chartsymbols.xml"): OpenCpnChartSymbolsSummary {
        val document = parseSecureXml(xml, sourceName)
        val root = document.documentElement
        val lookups = OpenCpnLookupRawParser.parseLookups(root)
        return OpenCpnChartSymbolsSummary(
            colorTables = parseColorTables(root),
            lookupCount = lookups.size,
            symbols = parseAssets(root, "symbol", OpenCpnRawAssetKind.Symbol),
            lineStyles = parseAssets(root, "line-style", OpenCpnRawAssetKind.LineStyle),
            patterns = parseAssets(root, "pattern", OpenCpnRawAssetKind.Pattern),
            lookups = lookups
        )
    }

    private fun parseSecureXml(xml: String, sourceName: String): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        return factory.newDocumentBuilder().parse(xml.byteInputStream()).also {
            require(it.documentElement?.tagName == "chartsymbols") {
                "Expected chartsymbols root in $sourceName but found ${it.documentElement?.tagName}"
            }
        }
    }

    private fun parseColorTables(root: Element): List<OpenCpnRawColorTable> =
        root.getElementsByTagName("color-table").asElements().map { table ->
            OpenCpnRawColorTable(
                name = table.getAttribute("name").ifBlank { table.getAttribute("palette") }.trim(),
                colors = table.descendantElements("color").mapNotNull { color ->
                    val name = color.getAttribute("name").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    OpenCpnRawColor(
                        name = name,
                        r = color.getAttribute("r").toIntOrNull() ?: return@mapNotNull null,
                        g = color.getAttribute("g").toIntOrNull() ?: return@mapNotNull null,
                        b = color.getAttribute("b").toIntOrNull() ?: return@mapNotNull null
                    )
                }
            )
        }

    private fun parseAssets(root: Element, tagName: String, kind: OpenCpnRawAssetKind): List<OpenCpnRawAsset> =
        root.getElementsByTagName(tagName).asElements().mapNotNull { asset ->
            val name = asset.childText("name") ?: asset.getAttribute("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OpenCpnRawAsset(
                rcid = asset.getAttribute("RCID").takeIf { it.isNotBlank() },
                name = name,
                kind = kind,
                description = asset.childText("description").orEmpty(),
                colorRefs = parseColorRefs(asset),
                bitmap = parseBitmap(asset.childElement("bitmap")),
                vector = parseVector(asset.childElement("vector"), asset.childText("HPGL")),
                definition = asset.childText("definition")
            )
        }

    private fun parseBitmap(bitmap: Element?): OpenCpnRawBitmap? {
        if (bitmap == null) return null
        val distance = bitmap.childElement("distance")
        return OpenCpnRawBitmap(
            width = bitmap.getAttribute("width").toIntOrNull(),
            height = bitmap.getAttribute("height").toIntOrNull(),
            pivot = bitmap.childElement("pivot")?.toPoint(),
            origin = bitmap.childElement("origin")?.toPoint(),
            graphicsLocation = bitmap.childElement("graphics-location")?.toPoint(),
            minDistance = distance?.getAttribute("min")?.toDoubleOrNull(),
            maxDistance = distance?.getAttribute("max")?.toDoubleOrNull()
        )
    }

    private fun parseVector(vector: Element?, fallbackHpgl: String?): OpenCpnRawVector? {
        if (vector == null && fallbackHpgl.isNullOrBlank()) return null
        val hpgl = vector?.childText("HPGL") ?: fallbackHpgl.orEmpty()
        return OpenCpnRawVector(
            width = vector?.getAttribute("width")?.toDoubleOrNull(),
            height = vector?.getAttribute("height")?.toDoubleOrNull(),
            pivot = vector?.childElement("pivot")?.toPoint(),
            origin = vector?.childElement("origin")?.toPoint(),
            hpgl = hpgl
        )
    }

    private fun parseColorRefs(asset: Element): List<String> = asset.descendantElements("color-ref")
        .flatMap { colorRef ->
            colorRef.textContent
                .split(',', ';', ' ', '\t', '\n', '\r')
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
        .distinct()

    private fun Element.toPoint(): OpenCpnRawPoint? {
        val x = getAttribute("x").toDoubleOrNull() ?: return null
        val y = getAttribute("y").toDoubleOrNull() ?: return null
        return OpenCpnRawPoint(x, y)
    }

    private fun Element.childText(name: String): String? = childElement(name)?.textContent?.trim()?.takeIf { it.isNotBlank() }

    private fun Element.childElement(name: String): Element? = childElements(name).firstOrNull()

    private fun Element.childElements(name: String): List<Element> = childNodes.asElements().filter { it.tagName == name }

    private fun Element.descendantElements(name: String): List<Element> {
        val result = mutableListOf<Element>()
        fun visit(element: Element) {
            element.childNodes.asElements().forEach { child ->
                if (child.tagName == name) result += child
                visit(child)
            }
        }
        visit(this)
        return result
    }

    private fun org.w3c.dom.NodeList.asElements(): List<Element> {
        val result = mutableListOf<Element>()
        for (i in 0 until length) (item(i) as? Element)?.let(result::add)
        return result
    }
}
