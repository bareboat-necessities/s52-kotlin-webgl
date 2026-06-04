package io.github.s52.core.draw

import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.settings.DisplayCategory

sealed interface S52DrawCommand {
    val featureId: Long
    val geometry: EncGeometry
    val priority: Int
    val viewingGroup: Int
    val category: DisplayCategory
    val overRadar: Boolean
    val kind: DrawCommandKind

    data class AreaFill(
        override val featureId: Long,
        override val geometry: EncGeometry,
        val colorToken: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand {
        override val kind: DrawCommandKind get() = DrawCommandKind.AreaFill
    }

    data class AreaPattern(
        override val featureId: Long,
        override val geometry: EncGeometry,
        val patternName: String,
        val parameters: List<String> = emptyList(),
        val backgroundColorToken: String? = null,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand {
        override val kind: DrawCommandKind get() = DrawCommandKind.AreaPattern
    }

    data class LineSimple(
        override val featureId: Long,
        override val geometry: EncGeometry,
        val style: String,
        val width: Double,
        val colorToken: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand {
        override val kind: DrawCommandKind get() = DrawCommandKind.LineSimple
    }

    data class LineComplex(
        override val featureId: Long,
        override val geometry: EncGeometry,
        val lineStyleName: String,
        val parameters: List<String> = emptyList(),
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand {
        override val kind: DrawCommandKind get() = DrawCommandKind.LineComplex
    }

    data class PointSymbol(
        override val featureId: Long,
        override val geometry: EncGeometry,
        val symbolName: String,
        val parameters: List<String> = emptyList(),
        val rotationDegrees: Double? = null,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand {
        override val kind: DrawCommandKind get() = DrawCommandKind.PointSymbol
    }

    data class Text(
        override val featureId: Long,
        override val geometry: EncGeometry,
        val textExpression: String,
        val rawArgs: List<String>,
        val textKind: InstructionKind = InstructionKind.TX,
        val colorToken: String? = null,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand {
        override val kind: DrawCommandKind get() = DrawCommandKind.Text
    }

    data class Sounding(
        override val featureId: Long,
        override val geometry: EncGeometry,
        val depthLabel: String,
        val colorToken: String,
        override val priority: Int,
        override val viewingGroup: Int,
        override val category: DisplayCategory,
        override val overRadar: Boolean
    ) : S52DrawCommand {
        override val kind: DrawCommandKind get() = DrawCommandKind.Sounding
    }
}
