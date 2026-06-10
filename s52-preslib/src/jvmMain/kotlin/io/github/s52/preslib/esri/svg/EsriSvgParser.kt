package io.github.s52.preslib.esri.svg

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object EsriSvgParser {
    private val allowedElements = setOf("svg", "g", "path", "title", "desc", "metadata")
    private val unsupportedFeatureAttributes = setOf(
        "filter", "mask", "clip-path", "clipPath", "marker-start", "marker-mid", "marker-end"
    )

    fun parse(file: File, category: String = "unknown"): EsriSvgDocument {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isIgnoringComments = true
            isCoalescing = true
        }
        val document = factory.newDocumentBuilder().parse(file)
        val root = document.documentElement
        require(root.tagName == "svg") { "Expected <svg> root in ${file.path}, found <${root.tagName}>" }

        val unsupportedElements = linkedSetOf<String>()
        val unsupportedFeatures = linkedSetOf<String>()
        val paths = mutableListOf<EsriSvgPath>()
        walk(root, inheritedTransform = null, unsupportedElements, unsupportedFeatures, paths)

        val unsupportedPathCommands = paths
            .flatMap { it.pathData.unsupportedCommands }
            .map { it.toString() }
            .distinct()
            .sorted()

        return EsriSvgDocument(
            sourceFile = file,
            category = category,
            widthRaw = root.attr("width").takeIf { it.isNotBlank() },
            heightRaw = root.attr("height").takeIf { it.isNotBlank() },
            widthMm = parseLengthMm(root.attr("width")),
            heightMm = parseLengthMm(root.attr("height")),
            viewBox = parseViewBox(root.attr("viewBox")),
            paths = paths,
            unsupportedElements = unsupportedElements.sorted(),
            unsupportedFeatures = unsupportedFeatures.sorted(),
            unsupportedPathCommands = unsupportedPathCommands
        )
    }

    private fun walk(
        element: Element,
        inheritedTransform: String?,
        unsupportedElements: MutableSet<String>,
        unsupportedFeatures: MutableSet<String>,
        paths: MutableList<EsriSvgPath>
    ) {
        if (element.tagName !in allowedElements) unsupportedElements += element.tagName
        unsupportedFeatureAttributes.forEach { attr ->
            if (element.hasAttribute(attr) && element.getAttribute(attr).isNotBlank()) unsupportedFeatures += "${element.tagName}@$attr"
        }
        if (element.tagName == "style") unsupportedFeatures += "style-element"
        if (element.tagName == "defs") unsupportedFeatures += "defs-element"
        val ownTransform = element.attr("transform").takeIf { it.isNotBlank() }
        val transform = listOfNotNull(inheritedTransform, ownTransform).joinToString(" ").takeIf { it.isNotBlank() }

        if (element.tagName == "path") {
            val style = parseStyle(element.attr("style"))
            val d = element.attr("d")
            val fill = element.attr("fill").takeIf { it.isNotBlank() } ?: style["fill"]
            val stroke = element.attr("stroke").takeIf { it.isNotBlank() } ?: style["stroke"]
            val strokeWidth = parseNumber(element.attr("stroke-width").takeIf { it.isNotBlank() } ?: style["stroke-width"])
            val fillRule = element.attr("fill-rule").takeIf { it.isNotBlank() } ?: style["fill-rule"]
            paths += EsriSvgPath(
                id = element.attr("id").takeIf { it.isNotBlank() },
                d = d,
                style = style,
                fill = fill,
                stroke = stroke,
                strokeWidth = strokeWidth,
                fillRule = fillRule,
                transform = transform,
                pathData = EsriSvgPathParser.parse(d)
            )
        }

        element.childNodes.asSequence()
            .filterIsInstance<Element>()
            .forEach { child -> walk(child, transform, unsupportedElements, unsupportedFeatures, paths) }
    }

    private fun parseStyle(style: String): Map<String, String> = style.split(';')
        .mapNotNull { item ->
            val idx = item.indexOf(':')
            if (idx <= 0) null else item.substring(0, idx).trim() to item.substring(idx + 1).trim()
        }
        .toMap()

    private fun parseViewBox(raw: String): EsriSvgViewBox? {
        val values = raw.trim().split(Regex("[\\s,]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.toDoubleOrNull() }
        return if (values.size == 4) EsriSvgViewBox(values[0], values[1], values[2], values[3]) else null
    }

    private fun parseLengthMm(raw: String): Double? {
        val trimmed = raw.trim().lowercase()
        if (trimmed.isBlank()) return null
        val number = Regex("[-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?").find(trimmed)?.value?.toDoubleOrNull() ?: return null
        return when {
            trimmed.endsWith("mm") -> number
            trimmed.endsWith("cm") -> number * 10.0
            trimmed.endsWith("in") -> number * 25.4
            trimmed.endsWith("pt") -> number * 25.4 / 72.0
            trimmed.endsWith("px") -> number * 25.4 / 96.0
            else -> number
        }
    }

    private fun parseNumber(raw: String?): Double? = raw
        ?.let { Regex("[-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?").find(it)?.value }
        ?.toDoubleOrNull()

    private fun Element.attr(name: String): String = getAttribute(name).orEmpty().trim()

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> = sequence {
        for (index in 0 until length) yield(item(index))
    }
}
