package io.github.s52.core.draw

import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import kotlin.math.abs

/**
 * Stable command transcript writer used by golden tests and renderer fixtures.
 *
 * This intentionally avoids JSON libraries so the core module remains tiny and
 * common-source friendly. The output is JSON-lines-like and deterministic.
 */
object S52DrawCommandTranscript {
    fun lines(commands: List<S52DrawCommand>): List<String> =
        commands.map(::line)

    fun serialize(commands: List<S52DrawCommand>): String =
        lines(commands).joinToString(separator = "\n", postfix = if (commands.isEmpty()) "" else "\n")

    fun line(command: S52DrawCommand): String {
        val fields = mutableListOf(
            "kind" to command.kind.stableName,
            "featureId" to command.featureId.toString(),
            "priority" to command.priority.toString(),
            "viewingGroup" to command.viewingGroup.toString(),
            "category" to command.category.name,
            "overRadar" to command.overRadar.toString(),
            "geometry" to geometry(command.geometry)
        )

        when (command) {
            is S52DrawCommand.AreaFill -> fields += "color" to command.colorToken
            is S52DrawCommand.AreaPattern -> {
                fields += "pattern" to command.patternName
                fields += "parameters" to list(command.parameters)
                command.backgroundColorToken?.let { fields += "background" to it }
            }
            is S52DrawCommand.LineSimple -> {
                fields += "style" to command.style
                fields += "width" to number(command.width)
                fields += "color" to command.colorToken
            }
            is S52DrawCommand.LineComplex -> {
                fields += "lineStyle" to command.lineStyleName
                fields += "parameters" to list(command.parameters)
            }
            is S52DrawCommand.PointSymbol -> {
                fields += "symbol" to command.symbolName
                fields += "parameters" to list(command.parameters)
                command.rotationDegrees?.let { fields += "rotation" to number(it) }
            }
            is S52DrawCommand.Text -> {
                fields += "textKind" to command.textKind.token
                fields += "text" to command.textExpression
                fields += "args" to list(command.rawArgs)
                command.colorToken?.let { fields += "color" to it }
            }
            is S52DrawCommand.Sounding -> {
                fields += "depth" to command.depthLabel
                fields += "color" to command.colorToken
            }
        }

        return fields.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
            "\"${escape(key)}\":\"${escape(value)}\""
        }
    }

    private fun geometry(geometry: EncGeometry): String = when (geometry) {
        is EncGeometry.Point -> "POINT(${coordinate(geometry.coordinate)})"
        is EncGeometry.MultiPoint -> "MULTIPOINT(${geometry.coordinates.joinToString(";") { coordinate(it) }})"
        is EncGeometry.LineString -> "LINESTRING(${geometry.coordinates.joinToString(";") { coordinate(it) }})"
        is EncGeometry.Polygon -> buildString {
            append("POLYGON(")
            append(geometry.outer.joinToString(";") { coordinate(it) })
            if (geometry.holes.isNotEmpty()) {
                append("|")
                append(geometry.holes.joinToString("|") { ring -> ring.joinToString(";") { coordinate(it) } })
            }
            append(")")
        }
    }

    private fun coordinate(coordinate: Coordinate): String = buildString {
        append(number(coordinate.lon))
        append(',')
        append(number(coordinate.lat))
        coordinate.z?.let {
            append(',')
            append(number(it))
        }
    }

    private fun list(values: List<String>): String = values.joinToString(prefix = "[", postfix = "]", separator = ",")

    private fun number(value: Double): String {
        if (abs(value) < 0.0000000005) return "0"
        val rounded = value.toLong()
        if (abs(value - rounded.toDouble()) < 0.0000000005) return rounded.toString()
        return value.toString().trimEnd('0').trimEnd('.')
    }

    private fun escape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
