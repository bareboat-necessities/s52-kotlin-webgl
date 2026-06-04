package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.SymbolDefinition
import io.github.s52.preslib.VectorCommand
import org.khronos.webgl.WebGLRenderingContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class SymbolRenderer(
    private val gl: WebGLRenderingContext,
    private val program: SolidColorProgram,
    private val presLib: PresLibPack
) {
    fun render(command: S52DrawCommand.PointSymbol, projector: GeometryProjector, colors: ColorResolver): Int {
        val geometry = command.geometry as? EncGeometry.Point ?: return 0
        val definition = presLib.symbols.find(command.symbolName) ?: return 0
        val anchor = projector.project(geometry.coordinate)
        val vertices = symbolLineSegments(
            definition = definition,
            anchor = anchor,
            projector = projector,
            rotationDegrees = command.rotationDegrees ?: 0.0
        )
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.5f)
        return program.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(null, fallback = "CHBLK"))
    }

    private fun symbolLineSegments(
        definition: SymbolDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ): FloatArray {
        val sx = projector.pixelToClipX(1.0)
        val sy = projector.pixelToClipY(1.0)
        val angle = rotationDegrees * PI / 180.0
        val ca = cos(angle)
        val sa = sin(angle)

        fun transform(x: Double, y: Double): ClipPoint {
            val lx = x - definition.pivotX
            val ly = y - definition.pivotY
            val rx = lx * ca - ly * sa
            val ry = lx * sa + ly * ca
            return ClipPoint(
                x = anchor.x + (rx * sx).toFloat(),
                y = anchor.y - (ry * sy).toFloat()
            )
        }

        val floats = ArrayList<Float>()
        var start: ClipPoint? = null
        var current: ClipPoint? = null
        for (command in definition.commands) {
            when (command) {
                is VectorCommand.MoveTo -> {
                    current = transform(command.x, command.y)
                    if (start == null) start = current
                }
                is VectorCommand.LineTo -> {
                    val next = transform(command.x, command.y)
                    val previous = current
                    if (previous != null) {
                        floats.add(previous.x); floats.add(previous.y)
                        floats.add(next.x); floats.add(next.y)
                    }
                    current = next
                }
                VectorCommand.ClosePath -> {
                    val previous = current
                    val first = start
                    if (previous != null && first != null) {
                        floats.add(previous.x); floats.add(previous.y)
                        floats.add(first.x); floats.add(first.y)
                    }
                    start = null
                    current = null
                }
            }
        }
        return floats.toFloatArray()
    }
}
