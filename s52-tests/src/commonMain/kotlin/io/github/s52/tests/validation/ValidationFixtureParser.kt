package io.github.s52.tests.validation

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.core.settings.S52Palette
import io.github.s52.core.settings.SymbolStyle
import io.github.s52.core.settings.BoundaryStyle

/** Parser for small checked-in or external command-level validation fixtures. */
object ValidationFixtureParser {
    fun parse(text: String): CommandValidationFixture {
        val metadata = linkedMapOf<String, String>()
        val settings = linkedMapOf<String, String>()
        val context = linkedMapOf<String, String>()
        val features = mutableListOf<EncFeature>()
        val expected = StringBuilder()
        var inExpected = false

        text.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

            if (line == "expect:") {
                inExpected = true
                return@forEachIndexed
            }

            if (inExpected) {
                expected.append(line).append('\n')
                return@forEachIndexed
            }

            when {
                line.startsWith("settings.") -> {
                    val (key, value) = splitKeyValue(line.removePrefix("settings."), lineNumber)
                    settings[key] = value
                }
                line.startsWith("context.") -> {
                    val (key, value) = splitKeyValue(line.removePrefix("context."), lineNumber)
                    context[key] = value
                }
                line.startsWith("feature=") -> {
                    features += parseFeature(line.removePrefix("feature="), lineNumber)
                }
                else -> {
                    val (key, value) = splitKeyValue(line, lineNumber)
                    metadata[key] = value
                }
            }
        }

        val id = metadata["id"] ?: error("Validation fixture is missing id")
        val source = ValidationFixtureSource.parse(metadata["source"] ?: ValidationFixtureSource.Custom.name)
        val description = metadata["description"] ?: ""
        val expectedTranscript = expected.toString()
        require(expectedTranscript.isNotBlank()) { "Validation fixture '$id' has no expected transcript" }

        return CommandValidationFixture(
            id = id,
            source = source,
            description = description,
            settings = parseSettings(settings),
            context = parseContext(context),
            features = features,
            expectedTranscript = expectedTranscript
        )
    }

    private fun splitKeyValue(line: String, lineNumber: Int): Pair<String, String> {
        val equals = line.indexOf('=')
        require(equals > 0) { "Line $lineNumber must be key=value: $line" }
        return line.substring(0, equals).trim() to line.substring(equals + 1).trim()
    }

    private fun parseSettings(values: Map<String, String>): MarinerSettings = MarinerSettings(
        displayCategory = values["displayCategory"]?.let { DisplayCategory.valueOf(it) } ?: DisplayCategory.Standard,
        palette = values["palette"]?.let { S52Palette.valueOf(it) } ?: S52Palette.DayBright,
        symbolStyle = values["symbolStyle"]?.let { SymbolStyle.valueOf(it) } ?: SymbolStyle.Simplified,
        boundaryStyle = values["boundaryStyle"]?.let { BoundaryStyle.valueOf(it) } ?: BoundaryStyle.Plain,
        safetyDepthMeters = values.double("safetyDepthMeters", 10.0),
        safetyContourMeters = values.double("safetyContourMeters", 10.0),
        shallowContourMeters = values.double("shallowContourMeters", 2.0),
        deepContourMeters = values.double("deepContourMeters", 30.0),
        showText = values.boolean("showText", true),
        showSoundings = values.boolean("showSoundings", true),
        showLightDescriptions = values.boolean("showLightDescriptions", true),
        scale = values.double("scale", 50_000.0),
        enabledViewingGroups = values["enabledViewingGroups"]?.let(::parseIntSet),
        disabledViewingGroups = values["disabledViewingGroups"]?.let(::parseIntSet) ?: emptySet()
    )

    private fun parseContext(values: Map<String, String>): PortrayalContext = PortrayalContext(
        compilationScale = values.double("compilationScale", 50_000.0),
        displayScale = values.double("displayScale", 50_000.0),
        viewportId = values["viewportId"] ?: "validation"
    )

    private fun Map<String, String>.double(key: String, default: Double): Double =
        this[key]?.toDouble() ?: default

    private fun Map<String, String>.boolean(key: String, default: Boolean): Boolean =
        this[key]?.toBooleanStrictOrNull() ?: default

    private fun parseIntSet(value: String): Set<Int> =
        value.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toInt() }.toSet()

    private fun parseFeature(value: String, lineNumber: Int): EncFeature {
        val parts = value.split('|')
        require(parts.size == 5) {
            "Line $lineNumber feature must be id|objectClass|primitive|attributes|geometry"
        }

        val id = parts[0].trim().toLong()
        val objectClass = S57ObjectClass.requireAcronym(parts[1].trim())
        val primitive = PrimitiveType.valueOf(parts[2].trim())
        val attributes = parseAttributes(parts[3].trim())
        val geometry = parseGeometry(parts[4].trim())

        return EncFeature(
            id = id,
            objectClass = objectClass,
            primitive = primitive,
            attributes = attributes,
            geometry = geometry
        )
    }

    private fun parseAttributes(value: String): S57Attributes {
        if (value == "-" || value.isBlank()) return S57Attributes.Empty
        val pairs = value.split(';').filter { it.isNotBlank() }.map { raw ->
            val equals = raw.indexOf('=')
            require(equals > 0) { "Invalid attribute assignment '$raw'" }
            val attribute = S57Attribute.requireAcronym(raw.substring(0, equals).trim())
            attribute to parseValue(raw.substring(equals + 1).trim())
        }
        return S57Attributes.of(*pairs.toTypedArray())
    }

    private fun parseValue(value: String): S57Value = when {
        value.startsWith("I:") -> S57Value.Integer(value.removePrefix("I:").toInt())
        value.startsWith("D:") -> S57Value.Decimal(value.removePrefix("D:").toDouble())
        value.startsWith("T:") -> S57Value.Text(value.removePrefix("T:"))
        value.startsWith("L:") -> {
            val rest = value.removePrefix("L:")
            S57Value.ListValue(rest.split(',').filter { it.isNotBlank() }.map { parseValue(it.trim()) })
        }
        value == "EMPTY" -> S57Value.Empty
        else -> error("Unknown S-57 fixture value '$value'; use I:, D:, T:, L:, or EMPTY")
    }

    private fun parseGeometry(value: String): EncGeometry = when {
        value.startsWith("POINT(") && value.endsWith(")") ->
            EncGeometry.Point(parseCoordinate(value.removePrefix("POINT(").removeSuffix(")")))
        value.startsWith("MULTIPOINT(") && value.endsWith(")") ->
            EncGeometry.MultiPoint(splitCoordinates(value.removePrefix("MULTIPOINT(").removeSuffix(")")))
        value.startsWith("LINESTRING(") && value.endsWith(")") ->
            EncGeometry.LineString(splitCoordinates(value.removePrefix("LINESTRING(").removeSuffix(")")))
        value.startsWith("POLYGON(") && value.endsWith(")") -> parsePolygon(value.removePrefix("POLYGON(").removeSuffix(")"))
        else -> error("Unsupported geometry '$value'")
    }

    private fun parsePolygon(body: String): EncGeometry.Polygon {
        val rings = body.split('|')
        val outer = splitCoordinates(rings.first())
        val holes = rings.drop(1).map(::splitCoordinates)
        return EncGeometry.Polygon(outer = outer, holes = holes)
    }

    private fun splitCoordinates(value: String): List<Coordinate> =
        value.split(';').filter { it.isNotBlank() }.map(::parseCoordinate)

    private fun parseCoordinate(value: String): Coordinate {
        val parts = value.split(',').map { it.trim() }
        require(parts.size == 2 || parts.size == 3) { "Coordinate must be lon,lat[,z]: '$value'" }
        return Coordinate(
            lon = parts[0].toDouble(),
            lat = parts[1].toDouble(),
            z = parts.getOrNull(2)?.toDouble()
        )
    }
}
