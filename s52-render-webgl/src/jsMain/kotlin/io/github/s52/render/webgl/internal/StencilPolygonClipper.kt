package io.github.s52.render.webgl.internal

import org.khronos.webgl.WebGLRenderingContext
import kotlin.math.max

/**
 * GPU even/odd polygon fill and clip helper.
 *
 * Phase 4 adds a per-polygon scissor rectangle.  Clearing the whole stencil
 * buffer for every ENC area is very expensive on dense harbour cells; limiting
 * stencil clear, ring writes, and pattern draws to the projected polygon bounds
 * avoids most of that cost and reduces fill bleed at the canvas edges.
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
        if (stencilCalls == 0) return 0

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
        val fillCalls = solidProgram.draw(WebGLRenderingContext.TRIANGLES, rect, color)
        finishStencilPass()
        return stencilCalls + fillCalls
    }

    fun clip(projected: ProjectedPolygonClip, projector: GeometryProjector, drawInside: () -> Int): Int {
        if (!stencilAvailable) return drawInside()
        val stencilCalls = writeStencil(projected, projector)
        if (stencilCalls == 0) return 0

        gl.colorMask(true, true, true, true)
        gl.stencilMask(0x00)
        gl.stencilFunc(WebGLRenderingContext.NOTEQUAL, 0, 0xFF)
        gl.stencilOp(
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.KEEP,
            WebGLRenderingContext.KEEP
        )
        val drawCalls = drawInside()
        finishStencilPass()
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
            WebGLRenderingContext.INVERT
        )

        var calls = drawRingFan(projected.outer)
        for (hole in projected.holes) calls += drawRingFan(hole)
        return calls
    }

    private fun drawRingFan(ring: List<ClipPoint>): Int {
        if (ring.size < 3) return 0
        val vertices = FloatArrayBuilder(max(6, ring.size * 2))
        for (point in ring) vertices.add(point.x, point.y)
        return solidProgram.draw(WebGLRenderingContext.TRIANGLE_FAN, vertices, STENCIL_MARK_COLOR)
    }

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
