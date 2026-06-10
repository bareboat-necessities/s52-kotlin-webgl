package io.github.s52.render.webgl.esri

import io.github.s52.preslib.esri.vector.EsriMesh

/** Shared low-level painter for generated ESRI vector meshes. */
internal class EsriWebGlMeshPainter(
    private val gl: dynamic,
    private val program: dynamic
) {
    fun drawMesh(vertices: FloatArray, indices: ShortArray, color: FloatArray) {
        val vertexBuffer = gl.createBuffer()
        gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer)
        gl.bufferData(gl.ARRAY_BUFFER, vertices, gl.STREAM_DRAW)

        val indexBuffer = gl.createBuffer()
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, indexBuffer)
        gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, indices, gl.STREAM_DRAW)

        gl.useProgram(program)
        val positionLocation = gl.getAttribLocation(program, "a_position")
        if (positionLocation >= 0) {
            gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer)
            gl.enableVertexAttribArray(positionLocation)
            gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0)
        }
        val colorLocation = gl.getUniformLocation(program, "u_color")
        if (colorLocation != null) gl.uniform4f(colorLocation, color[0], color[1], color[2], color[3])
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, indexBuffer)
        gl.drawElements(gl.TRIANGLES, indices.size, gl.UNSIGNED_SHORT, 0)
    }
}

internal data class EsriScreenPoint(val x: Double, val y: Double)

internal fun EsriMesh.transformedVertices(transform: (Double, Double) -> EsriScreenPoint, viewportWidth: Double, viewportHeight: Double): FloatArray {
    val out = FloatArray(vertices.size)
    var i = 0
    while (i + 1 < vertices.size) {
        val p = transform(vertices[i].toDouble(), vertices[i + 1].toDouble())
        out[i] = ((p.x / viewportWidth) * 2.0 - 1.0).toFloat()
        out[i + 1] = (1.0 - (p.y / viewportHeight) * 2.0).toFloat()
        i += 2
    }
    return out
}
