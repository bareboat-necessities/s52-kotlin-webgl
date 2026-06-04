package io.github.s52.render.webgl.internal

import org.khronos.webgl.WebGL2RenderingContext
import org.khronos.webgl.WebGLBuffer
import org.khronos.webgl.WebGLProgram
import org.khronos.webgl.WebGLShader
import org.khronos.webgl.WebGLUniformLocation

internal class SolidColorProgram(
    private val gl: WebGL2RenderingContext
) {
    private val program: WebGLProgram = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    private val positionLocation: Int = gl.getAttribLocation(program, "a_position")
    private val colorLocation: WebGLUniformLocation =
        gl.getUniformLocation(program, "u_color") ?: error("Missing u_color uniform")
    private val buffer: WebGLBuffer = gl.createBuffer() ?: error("Could not create WebGL buffer")

    fun draw(mode: Int, vertices: FloatArray, color: GlColor): Int {
        if (vertices.size < 2) return 0
        require(vertices.size % 2 == 0) { "SolidColorProgram expects x/y vertex pairs" }
        val vertexCount = vertices.size / 2

        gl.useProgram(program)
        gl.bindBuffer(WebGL2RenderingContext.ARRAY_BUFFER, buffer)
        gl.bufferData(WebGL2RenderingContext.ARRAY_BUFFER, vertices.toFloat32Array(), WebGL2RenderingContext.STREAM_DRAW)
        gl.enableVertexAttribArray(positionLocation)
        gl.vertexAttribPointer(positionLocation, 2, WebGL2RenderingContext.FLOAT, false, 0, 0)
        gl.uniform4f(colorLocation, color.r, color.g, color.b, color.a)
        gl.drawArrays(mode, 0, vertexCount)
        return 1
    }

    private fun compileShader(type: Int, source: String): WebGLShader {
        val shader = gl.createShader(type) ?: error("Could not create shader")
        gl.shaderSource(shader, source)
        gl.compileShader(shader)
        val ok = gl.getShaderParameter(shader, WebGL2RenderingContext.COMPILE_STATUS) as Boolean
        if (!ok) {
            val log = gl.getShaderInfoLog(shader) ?: "unknown error"
            gl.deleteShader(shader)
            error("WebGL shader compile failed: $log")
        }
        return shader
    }

    private fun linkProgram(vertexSource: String, fragmentSource: String): WebGLProgram {
        val vertexShader = compileShader(WebGL2RenderingContext.VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(WebGL2RenderingContext.FRAGMENT_SHADER, fragmentSource)
        val program = gl.createProgram() ?: error("Could not create WebGL program")
        gl.attachShader(program, vertexShader)
        gl.attachShader(program, fragmentShader)
        gl.linkProgram(program)
        val ok = gl.getProgramParameter(program, WebGL2RenderingContext.LINK_STATUS) as Boolean
        gl.deleteShader(vertexShader)
        gl.deleteShader(fragmentShader)
        if (!ok) {
            val log = gl.getProgramInfoLog(program) ?: "unknown error"
            gl.deleteProgram(program)
            error("WebGL program link failed: $log")
        }
        return program
    }

    private companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 a_position;
            void main() {
                gl_Position = vec4(a_position, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_color;
            void main() {
                gl_FragColor = u_color;
            }
        """
    }
}
