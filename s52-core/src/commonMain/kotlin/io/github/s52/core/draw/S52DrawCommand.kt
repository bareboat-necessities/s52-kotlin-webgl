package io.github.s52.core.draw

import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.settings.DisplayCategory

sealed interface S52DrawCommand {
    val featureId: Long
    val priority: Int
    val viewingGroup: Int
    val category: DisplayCategory
    val overRadar: Boolean

    data class AreaFill(
        override val featureId: Long,
        val geometry: EncGeometry,
        val colorToken: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand

    data class AreaPattern(
        override val featureId: Long,
        val geometry: EncGeometry,
        val patternName: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand

    data class LineSimple(
        override val featureId: Long,
        val geometry: EncGeometry,
        val style: String,
        val width: Double,
        val colorToken: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand

    data class LineComplex(
        override val featureId: Long,
        val geometry: EncGeometry,
        val lineStyleName: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand

    data class PointSymbol(
        override val featureId: Long,
        val geometry: EncGeometry,
        val symbolName: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand

    data class Text(
        override val featureId: Long,
        val geometry: EncGeometry,
        val textExpression: String,
        val rawArgs: List<String>,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand
}
