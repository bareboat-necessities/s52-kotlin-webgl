package io.github.s52.api

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.settings.DisplayCategory

/**
 * Deterministic command-level fixtures for browser/headless WebGL screenshots.
 *
 * These fixtures intentionally exercise failure modes that are hard to catch in
 * command-transcript tests: concave area clipping, holes, HPGL pattern tiles,
 * and high-priority point symbols that used to fall back or disappear.
 */
object S52VisualRegressionFixtures {
    val requiredSymbolNames: List<String> = listOf("TOPMAR88", "WRECKS05", "OBSTRN11", "LIGHTS11", "QUESMRK1")
    val requiredPatternNames: List<String> = listOf("MARSHES1", "FSHFAC04", "QUESMRK1")

    fun Commands(includeLabels: Boolean = true): List<S52DrawCommand> {
        val commands = mutableListOf<S52DrawCommand>()
        var featureId = 33_000L

        fun addLabel(text: String, lon: Double, lat: Double) {
            if (!includeLabels) return
            commands += S52DrawCommand.Text(
                featureId = featureId++,
                geometry = EncGeometry.Point(Coordinate(lon, lat)),
                textExpression = text,
                rawArgs = listOf(text),
                textKind = InstructionKind.TX,
                colorToken = "CHBLK",
                priority = 100,
                viewingGroup = 99999,
                category = DisplayCategory.Other,
                overRadar = true
            )
        }

        val concaveWithHole = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.100, 40.000),
                Coordinate(-73.930, 40.000),
                Coordinate(-73.930, 40.050),
                Coordinate(-74.020, 40.050),
                Coordinate(-74.020, 40.100),
                Coordinate(-73.930, 40.100),
                Coordinate(-73.930, 40.155),
                Coordinate(-74.100, 40.155),
                Coordinate(-74.100, 40.000)
            ),
            holes = listOf(
                listOf(
                    Coordinate(-74.075, 40.035),
                    Coordinate(-74.045, 40.035),
                    Coordinate(-74.045, 40.070),
                    Coordinate(-74.075, 40.070),
                    Coordinate(-74.075, 40.035)
                )
            )
        )
        commands += S52DrawCommand.AreaFill(featureId++, concaveWithHole, "DEPMD", 1, 33010, DisplayCategory.Other, false)
        commands += S52DrawCommand.AreaPattern(featureId++, concaveWithHole, "MARSHES1", emptyList(), "DEPMD", 2, 33010, DisplayCategory.Other, false)
        addLabel("concave polygon + hole + HPGL MARSHES1", -74.100, 39.985)

        val patternTiles = listOf(
            "FSHFAC04" to box(-73.900, 40.115, 0.070, 0.050),
            "QUESMRK1" to box(-73.805, 40.115, 0.070, 0.050),
            "MARSHES1" to box(-73.710, 40.115, 0.070, 0.050)
        )
        for ((patternName, polygon) in patternTiles) {
            commands += S52DrawCommand.AreaFill(featureId++, polygon, "DEPDW", 1, 33011, DisplayCategory.Other, false)
            commands += S52DrawCommand.AreaPattern(featureId++, polygon, patternName, emptyList(), "DEPDW", 2, 33011, DisplayCategory.Other, false)
            addLabel(patternName, polygon.outer.first().lon, polygon.outer.first().lat - 0.010)
        }

        val symbols = listOf(
            "TOPMAR88" to Coordinate(-74.060, 40.205),
            "WRECKS05" to Coordinate(-74.010, 40.205),
            "OBSTRN11" to Coordinate(-73.960, 40.205),
            "LIGHTS11" to Coordinate(-73.910, 40.205),
            "QUESMRK1" to Coordinate(-73.860, 40.205)
        )
        for ((name, point) in symbols) {
            commands += S52DrawCommand.PointSymbol(
                featureId = featureId++,
                geometry = EncGeometry.Point(point),
                symbolName = name,
                priority = 9,
                viewingGroup = 33012,
                category = DisplayCategory.Other,
                overRadar = true
            )
            addLabel(name, point.lon - 0.018, point.lat - 0.020)
        }

        return commands
    }

    private fun box(left: Double, top: Double, width: Double, height: Double): EncGeometry.Polygon {
        val right = left + width
        val bottom = top - height
        return EncGeometry.Polygon(
            outer = listOf(
                Coordinate(left, top),
                Coordinate(right, top),
                Coordinate(right, bottom),
                Coordinate(left, bottom),
                Coordinate(left, top)
            )
        )
    }
}
