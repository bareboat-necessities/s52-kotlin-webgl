package io.github.s52.preslib.esri.importer

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

internal data class EsriConditionMap(
    val name: String,
    val alias: String,
    val symbolScale: String,
    val version: String,
    val features: List<EsriFeatureRule>
) {
    val directSymbolConditionCount: Int get() = features.sumOf { feature -> feature.conditions.count { it.symbolName != null } }
    val functionConditionCount: Int get() = features.sumOf { feature -> feature.conditions.count { it.functionNames.isNotEmpty() } }
    val objectNames: Set<String> get() = features.flatMap { it.objects }.toSortedSet()
    val functionNames: Set<String> get() = features.flatMap { it.conditions.flatMap { condition -> condition.functionNames } }.toSortedSet()
    val symbolNames: Set<String> get() = features.flatMap { it.conditions.mapNotNull { condition -> condition.symbolName } }.toSortedSet()
}

internal data class EsriFeatureRule(
    val objects: List<String>,
    val primitive: Int?,
    val conditions: List<EsriCondition>
)

internal data class EsriCondition(
    val symbolName: String?,
    val functionNames: List<String>,
    val rules: List<EsriRule>
)

internal data class EsriRule(
    val type: String,
    val field: String?,
    val objectName: String?,
    val operator: String,
    val values: List<String>,
    val display: String?
)

internal object EsriCustomSymbolMapParser {
    fun parse(file: File): EsriConditionMap {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isIgnoringComments = true
            isCoalescing = true
        }
        val document = factory.newDocumentBuilder().parse(file)
        val root = document.documentElement
        require(root.tagName == "conditionMap") { "Expected <conditionMap> root in ${file.path}, found <${root.tagName}>" }
        val features = root.childElements("feature").map { feature ->
            EsriFeatureRule(
                objects = feature.attr("object").split(',').map { it.trim() }.filter { it.isNotEmpty() },
                primitive = feature.attr("prim").takeIf { it.isNotBlank() }?.toIntOrNull(),
                conditions = feature.childElements("condition").map { condition ->
                    EsriCondition(
                        symbolName = condition.attr("symbolName").takeIf { it.isNotBlank() },
                        functionNames = condition.attr("functionName")
                            .split(':', ',', ';')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() },
                        rules = condition.childElements("rule").map { rule ->
                            EsriRule(
                                type = rule.attr("type"),
                                field = rule.attr("field").takeIf { it.isNotBlank() },
                                objectName = rule.attr("object").takeIf { it.isNotBlank() },
                                operator = rule.attr("operator"),
                                values = rule.values(),
                                display = rule.attr("display").takeIf { it.isNotBlank() }
                            )
                        }
                    )
                }
            )
        }
        return EsriConditionMap(
            name = root.attr("name"),
            alias = root.attr("alias"),
            symbolScale = root.attr("symbolScale"),
            version = root.attr("version"),
            features = features
        )
    }

    private fun Element.attr(name: String): String = getAttribute(name).orEmpty().trim()

    private fun Element.childElements(tagName: String): List<Element> = childNodes.asSequence()
        .filterIsInstance<Element>()
        .filter { it.tagName == tagName }
        .toList()

    private fun Element.values(): List<String> {
        val attrValue = attr("value").takeIf { it.isNotBlank() }
        val nested = childElements("value").map { it.textContent.trim() }.filter { it.isNotEmpty() }
        return buildList {
            if (attrValue != null) add(attrValue)
            addAll(nested)
        }
    }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> = sequence {
        for (index in 0 until length) yield(item(index))
    }
}
