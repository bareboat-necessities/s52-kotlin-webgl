package io.github.s52.render.webgl.internal

import org.khronos.webgl.WebGLBuffer
import org.khronos.webgl.WebGLProgram
import org.khronos.webgl.WebGLRenderingContext
import org.khronos.webgl.WebGLShader
import org.khronos.webgl.WebGLTexture
import org.khronos.webgl.WebGLUniformLocation

/** Draws one textured quad from a raster-symbol atlas. */
internal class TextureProgram(
    private val gl: WebGLRenderingContext
) {
    private val program: WebGLProgram = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    private val positionLocation: Int = gl.getAttribLocation(program, "a_position")
    private val texCoordLocation: Int = gl.getAttribLocation(program, "a_texCoord")
    private val textureLocation: WebGLUniformLocation =
        gl.getUniformLocation(program, "u_texture") ?: error("Missing u_texture uniform")
    private val alphaLocation: WebGLUniformLocation =
        gl.getUniformLocation(program, "u_alpha") ?: error("Missing u_alpha uniform")
    private val buffer: WebGLBuffer = gl.createBuffer() ?: error("Could not create WebGL buffer")

    /**
     * [vertices] is an interleaved x/y/u/v triangle list.
     */
    fun drawTriangles(texture: WebGLTexture, vertices: FloatArray, alpha: Float = 1.0f): Int {
        if (vertices.size < 24) return 0
        require(vertices.size % 4 == 0) { "TextureProgram expects interleaved x/y/u/v vertices" }
        val vertexCount = vertices.size / 4

        gl.useProgram(program)
        gl.activeTexture(WebGLRenderingContext.TEXTURE0)
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture)
        gl.uniform1i(textureLocation, 0)
        gl.uniform1f(alphaLocation, alpha)

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, buffer)
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertices.toFloat32Array(), WebGLRenderingContext.STREAM_DRAW)

        gl.enableVertexAttribArray(positionLocation)
        gl.vertexAttribPointer(positionLocation, 2, WebGLRenderingContext.FLOAT, false, 16, 0)
        gl.enableVertexAttribArray(texCoordLocation)
        gl.vertexAttribPointer(texCoordLocation, 2, WebGLRenderingContext.FLOAT, false, 16, 8)

        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, vertexCount)
        return 1
    }

    private fun compileShader(type: Int, source: String): WebGLShader {
        val shader = gl.createShader(type) ?: error("Could not create shader")
        gl.shaderSource(shader, source)
        gl.compileShader(shader)
        val ok = gl.getShaderParameter(shader, WebGLRenderingContext.COMPILE_STATUS) as Boolean
        if (!ok) {
            val log = gl.getShaderInfoLog(shader) ?: "unknown error"
            gl.deleteShader(shader)
            error("WebGL texture shader compile failed: $log")
        }
        return shader
    }

    private fun linkProgram(vertexSource: String, fragmentSource: String): WebGLProgram {
        val vertexShader = compileShader(WebGLRenderingContext.VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource)
        val program = gl.createProgram() ?: error("Could not create program")
        gl.attachShader(program, vertexShader)
        gl.attachShader(program, fragmentShader)
        gl.linkProgram(program)
        val ok = gl.getProgramParameter(program, WebGLRenderingContext.LINK_STATUS) as Boolean
        gl.deleteShader(vertexShader)
        gl.deleteShader(fragmentShader)
        if (!ok) {
            val log = gl.getProgramInfoLog(program) ?: "unknown error"
            gl.deleteProgram(program)
            error("WebGL texture program link failed: $log")
        }
        return program
    }

    private companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 a_position;
            attribute vec2 a_texCoord;
            varying vec2 v_texCoord;
            void main() {
                gl_Position = vec4(a_position, 0.0, 1.0);
                v_texCoord = a_texCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            uniform float u_alpha;
            varying vec2 v_texCoord;
            void main() {
                vec4 texel = texture2D(u_texture, v_texCoord);
                gl_FragColor = vec4(texel.rgb, texel.a * u_alpha);
            }
        """
    }
}
