package io.github.s52.render.webgl

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.preslib.PresLibPack
import org.khronos.webgl.WebGL2RenderingContext
import org.w3c.dom.HTMLCanvasElement

/**
 * Phase 0 WebGL2 renderer placeholder.
 *
 * It validates WebGL2 availability, clears the canvas using the active palette,
 * and reports command counts. Phase 8 turns this into a full S-52 renderer.
 */
class WebGlS52Renderer(
    private val canvas: HTMLCanvasElement,
    private val presLib: PresLibPack
) {
    private val gl: WebGL2RenderingContext =
        canvas.getContext("webgl2") as? WebGL2RenderingContext
            ?: error("WebGL2 is not available in this browser")

    fun render(commands: List<S52DrawCommand>, settings: MarinerSettings): RenderStats {
        resizeToDisplaySize()
        gl.viewport(0, 0, canvas.width, canvas.height)

        val background = presLib.colors.color(settings.palette, "DEPDW")
        if (background != null) {
            gl.clearColor(background.r / 255.0f, background.g / 255.0f, background.b / 255.0f, 1.0f)
        } else {
            gl.clearColor(0.9f, 0.95f, 1.0f, 1.0f)
        }
        gl.clear(WebGL2RenderingContext.COLOR_BUFFER_BIT)

        return RenderStats(
            areaFillCount = commands.count { it is S52DrawCommand.AreaFill },
            areaPatternCount = commands.count { it is S52DrawCommand.AreaPattern },
            lineCount = commands.count { it is S52DrawCommand.LineSimple || it is S52DrawCommand.LineComplex },
            symbolCount = commands.count { it is S52DrawCommand.PointSymbol },
            textCount = commands.count { it is S52DrawCommand.Text },
            soundingCount = commands.count { it is S52DrawCommand.Sounding }
        )
    }

    private fun resizeToDisplaySize() {
        val displayWidth = canvas.clientWidth.coerceAtLeast(1)
        val displayHeight = canvas.clientHeight.coerceAtLeast(1)
        if (canvas.width != displayWidth || canvas.height != displayHeight) {
            canvas.width = displayWidth
            canvas.height = displayHeight
        }
    }
}

data class RenderStats(
    val areaFillCount: Int,
    val areaPatternCount: Int,
    val lineCount: Int,
    val symbolCount: Int,
    val textCount: Int,
    val soundingCount: Int = 0
)
