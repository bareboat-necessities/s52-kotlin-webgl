package io.github.s52.render.webgl.internal

import org.khronos.webgl.WebGLRenderingContext
import kotlin.math.max

/**
 * GPU even/odd polygon fill and clip helper.
 *
 * The old CPU trapezoid decomposition emits long horizontal bands.  It is
 * deterministic, but for very complex ENC polygons those bands are expensive to
 * build and precision mistakes are visually obvious as horizontal colour leaks.
 *
 * When the WebGL context has a stencil buffer, this helper renders every ring as
 * an invisible TRIANGLE_FAN with INVERT stencil op, then renders either the
 * polygon bounding rectangle or a caller-supplied pattern pass through
 * stencil != 0.  The technique is the standard even/odd stencil fill for
 * concave polygons and holes and avoids CPU triangulation on the hot path.
 */
internal class StencilPolygonClipper(
    private val gl: WebGLRenderingContext,
    private val solidProgram: SolidColorProgram
) {
    private val stencilAvailable: Boolean = detectStencilBits() > 0

    fun isAvailable(): Boolean = stencilAvailable

    fun fill(projected: ProjectedPolygonClip, color: GlColor): Int {
        if (!stencilAvailable) return 0
        val bounds = projected.bounds ?: return 0
        val stencilCalls = writeStencil(projected)
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

    fun clip(projected: ProjectedPolygonClip, drawInside: () -> Int): Int {
        if (!stencilAvailable) return drawInside()
        val stencilCalls = writeStencil(projected)
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

    private fun writeStencil(projected: ProjectedPolygonClip): Int {
        if (projected.outer.size < 3) return 0

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
