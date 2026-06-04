package io.github.s52.api

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import kotlin.math.max

enum class S52GallerySection(val route: String) {
    Chart("chart"), Symbols("symbols"), Lines("lines"), Patterns("patterns"), Colors("colors"), All("all");
    companion object {
        fun fromHash(hash: String?): S52GallerySection {
            val value = hash.orEmpty().removePrefix("#").trim().lowercase()
            return entries.firstOrNull { it.route == value } ?: Chart
        }
    }
}

data class S52GalleryRequest(
    val section: S52GallerySection = S52GallerySection.All,
    val palette: S52Palette = S52Palette.DayBright,
    val includeLabels: Boolean = true,
    val columns: Int = 4,
    val cellWidth: Double = 1.0,
    val cellHeight: Double = 1.0
)

data class S52GalleryResult(
    val title: String,
    val section: S52GallerySection,
    val commands: List<S52DrawCommand>
) {
    val assetCommandCount: Int get() = commands.count { it !is S52DrawCommand.Text }
    val totalCommandCount: Int get() = commands.size
}

object S52GalleryBuilder {
    fun build(presLib: PresLibPack, request: S52GalleryRequest = S52GalleryRequest()): S52GalleryResult {
        val commands = mutableListOf<S52DrawCommand>()
        var nextFeatureId = 20_000L
        var currentRow = 0.0
        val columns = max(1, request.columns)

        fun addText(label: String, x: Double, y: Double) {
            if (!request.includeLabels) return
            commands += S52DrawCommand.Text(
                featureId = nextFeatureId++, geometry = EncGeometry.Point(Coordinate(x, y)),
                textExpression = label, rawArgs = listOf(label), textKind = InstructionKind.TX,
                colorToken = "CHBLK", priority = 100, viewingGroup = 99999,
                category = DisplayCategory.Other, overRadar = true
            )
        }
        fun sectionHeader(label: String) { addText(label, 0.0, -currentRow * request.cellHeight); currentRow += 0.8 }
        fun rows(count: Int): Int = (count + columns - 1) / columns

        fun addSymbols() {
            val symbols = presLib.symbols.all()
            sectionHeader("Symbols (${symbols.size})")
            val baseRow = currentRow
            symbols.forEachIndexed { index, symbol ->
                val col = index % columns; val row = index / columns
                val x = col * request.cellWidth; val y = -(baseRow + row * request.cellHeight)
                commands += S52DrawCommand.PointSymbol(
                    featureId = nextFeatureId++, geometry = EncGeometry.Point(Coordinate(x, y)),
                    symbolName = symbol.name, priority = 10, viewingGroup = 99001,
                    category = DisplayCategory.Other, overRadar = true
                )
                addText(symbol.name, x + 0.08, y - 0.18)
            }
            currentRow = baseRow + rows(symbols.size) * request.cellHeight + 1.0
        }

        fun addLines() {
            val styles = presLib.lineStyles.all()
            sectionHeader("Line styles (${styles.size})")
            val baseRow = currentRow
            styles.forEachIndexed { index, style ->
                val y = -(baseRow + index * request.cellHeight * 0.75)
                commands += S52DrawCommand.LineComplex(
                    featureId = nextFeatureId++,
                    geometry = EncGeometry.LineString(listOf(Coordinate(0.0, y), Coordinate(request.cellWidth * 1.6, y))),
                    lineStyleName = style.name, priority = 10, viewingGroup = 99002,
                    category = DisplayCategory.Other, overRadar = true
                )
                addText(style.name, request.cellWidth * 1.8, y)
            }
            currentRow = baseRow + styles.size * request.cellHeight * 0.75 + 1.0
        }

        fun addPatterns() {
            val patterns = presLib.patterns.all()
            sectionHeader("Patterns (${patterns.size})")
            val baseRow = currentRow
            patterns.forEachIndexed { index, pattern ->
                val col = index % columns; val row = index / columns
                val left = col * request.cellWidth; val top = -(baseRow + row * request.cellHeight)
                val polygon = box(left, top, request.cellWidth * 0.7, request.cellHeight * 0.45)
                commands += S52DrawCommand.AreaFill(nextFeatureId++, polygon, "DEPDW", 1, 99003, DisplayCategory.Other, false)
                commands += S52DrawCommand.AreaPattern(nextFeatureId++, polygon, pattern.name, emptyList(), null, 2, 99003, DisplayCategory.Other, false)
                addText(pattern.name, left, top - 0.60)
            }
            currentRow = baseRow + rows(patterns.size) * request.cellHeight + 1.0
        }

        fun addColors() {
            val colors = presLib.colors.all(request.palette)
            sectionHeader("Palette ${request.palette.name} (${colors.size} colors)")
            val baseRow = currentRow
            colors.forEachIndexed { index, color ->
                val col = index % columns; val row = index / columns
                val left = col * request.cellWidth; val top = -(baseRow + row * request.cellHeight)
                commands += S52DrawCommand.AreaFill(nextFeatureId++, box(left, top, request.cellWidth * 0.7, request.cellHeight * 0.45), color.token, 1, 99004, DisplayCategory.Other, false)
                addText(color.token, left, top - 0.60)
            }
            currentRow = baseRow + rows(colors.size) * request.cellHeight + 1.0
        }

        when (request.section) {
            S52GallerySection.Chart -> {}
            S52GallerySection.Symbols -> addSymbols()
            S52GallerySection.Lines -> addLines()
            S52GallerySection.Patterns -> addPatterns()
            S52GallerySection.Colors -> addColors()
            S52GallerySection.All -> { addSymbols(); addLines(); addPatterns(); addColors() }
        }
        return S52GalleryResult(
            title = when (request.section) {
                S52GallerySection.Chart -> "Portrayal chart demo"
                S52GallerySection.Symbols -> "S-52 symbol gallery"
                S52GallerySection.Lines -> "S-52 line-style gallery"
                S52GallerySection.Patterns -> "S-52 pattern gallery"
                S52GallerySection.Colors -> "S-52 palette gallery"
                S52GallerySection.All -> "S-52 library gallery"
            },
            section = request.section,
            commands = commands
        )
    }

    private fun box(left: Double, top: Double, width: Double, height: Double): EncGeometry.Polygon {
        val right = left + width; val bottom = top - height
        return EncGeometry.Polygon(listOf(Coordinate(left, top), Coordinate(right, top), Coordinate(right, bottom), Coordinate(left, bottom), Coordinate(left, top)))
    }
}
