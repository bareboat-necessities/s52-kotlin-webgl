package io.github.s52.render.webgl.internal

import io.github.s52.core.geometry.PolygonTriangulator
import io.github.s52.core.geometry.TriangulationPoint
import org.khronos.webgl.WebGLRenderingContext
import kotlin.math.max

/**
 * GPU polygon fill and clip helper.
 *
 * Each projected polygon is triangulated once into the stencil mask, then the
 * final color or pattern draw is clipped by that mask and a tight scissor box.
 * The triangulated mask is more robust than a triangle-fan ring toggle for
 * concave OpenCPN/S-57 areas and prevents color from leaking outside rings or
 * into holes while keeping fragment work bounded to the polygon extent.
 */
internal class StencilPolygonClipper(
    private val gl: WebGLRenderingContext,
    private val solidProgram: SolidColorProgram
) {
    private val stencilAvailable: Boolean = detectStencilBits() > 0

    fun isAvailable(): Boolean = stencilAvailable

    fun fill(projected: ProjectedPolygonClip, projector: GeometryProjector, color: GlColor): Int {
        if (!stencilAvailable) return 0
        val bounds = projected.bounds ?: return 0
        val stencilCalls = writeStencil(projected, projector)
        if (stencilCalls == 0) {
            finishStencilPass()
            return 0
        }

        gl.colorMask(true, true, true, true)
        gl.stencilMask(0x00)
        gl.stencilFunc(WebGLRenderingContext.NOTEQUAL, 0, 0xFF)
        gl.stencilOp(
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.KEEP
        )

        val rect = FloatArrayBuilder(12)
        rect.addTriangle(
            ClipPoint(bounds.minX, bounds.minY),
            ClipPoint(bounds.maxX, bounds.minY),
            ClipPoint(bounds.maxX, bounds.maxY)
        )
        rect.addTriangle(
            ClipPoint(bounds.minX, bounds.minY),
            ClipPoint(bounds.maxX, bounds.maxY),
            ClipPoint(bounds.minX, bounds.maxY)
        )
        val fillCalls = try {
            solidProgram.draw(WebGLRenderingContext.TRIANGLES, rect, color)
        } finally {
            finishStencilPass()
        }
        return stencilCalls + fillCalls
    }

    fun clip(projected: ProjectedPolygonClip, projector: GeometryProjector, drawInside: () -> Int): Int {
        if (!stencilAvailable) return drawInside()
        val stencilCalls = writeStencil(projected, projector)
        if (stencilCalls == 0) {
            finishStencilPass()
            return 0
        }

        gl.colorMask(true, true, true, true)
        gl.stencilMask(0x00)
        gl.stencilFunc(WebGLRenderingContext.NOTEQUAL, 0, 0xFF)
        gl.stencilOp(
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.KEEP
        )
        val drawCalls = try {
            drawInside()
        } finally {
            finishStencilPass()
        }
        return stencilCalls + drawCalls
    }

    private fun writeStencil(projected: ProjectedPolygonClip, projector: GeometryProjector): Int {
        if (projected.outer.size < 3) return 0
        val bounds = projected.bounds ?: return 0
        val scissor = projector.scissorFor(bounds) ?: return 0

        gl.enable(WebGLRenderingContext.SCISSOR_TEST)
        gl.scissor(scissor.x, scissor.y, scissor.width, scissor.height)
        gl.enable(WebGLRenderingContext.STENCIL_TEST)
        gl.clearStencil(0)
        gl.stencilMask(0xFF)
        gl.clear(WebGLRenderingContext.STENCIL_BUFFER_BIT)

        gl.colorMask(false, false, false, false)
        gl.stencilFunc(WebGLRenderingContext.ALWAYS, 1, 0xFF)
        gl.stencilOp(
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.REPLACE
        )

        return drawTriangulatedMask(projected)
    }

    private fun drawTriangulatedMask(projected: ProjectedPolygonClip): Int {
        val outer = projected.outer.toTriangulationRing()
        if (outer.size < 3) return 0
        val holes = projected.holes.map { it.toTriangulationRing() }.filter { it.size >= 3 }
        val triangles = PolygonTriangulator.triangulate(outer, holes)
        if (triangles.isEmpty()) return 0
        val vertices = FloatArrayBuilder(max(6, triangles.size * 6))
        for (triangle in triangles) {
            vertices.add(triangle.a.x.toFloat(), triangle.a.y.toFloat())
            vertices.add(triangle.b.x.toFloat(), triangle.b.y.toFloat())
            vertices.add(triangle.c.x.toFloat(), triangle.c.y.toFloat())
        }
        return solidProgram.draw(WebGLRenderingContext.TRIANGLES, vertices, STENCIL_MARK_COLOR)
    }

    private fun List<ClipPoint>.toTriangulationRing(): List<TriangulationPoint> =
        map { TriangulationPoint(it.x.toDouble(), it.y.toDouble()) }

    private fun finishStencilPass() {
        gl.stencilMask(0xFF)
        gl.disable(WebGLRenderingContext.STENCIL_TEST)
        gl.disable(WebGLRenderingContext.SCISSOR_TEST)
        gl.colorMask(true, true, true, true)
    }

    private fun detectStencilBits(): Int {
        val bits = gl.getParameter(WebGLRenderingContext.STENCIL_BITS)
        return when (bits) {
            is Int -> bits
            is Number -> bits.toInt()
            else -> 0
        }
    }

    private companion object {
        private val STENCIL_MARK_COLOR = GlColor(0.0f, 0.0f, 0.0f, 0.0f)
    }
}
