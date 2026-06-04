package io.github.s52.render.webgl.internal

import org.khronos.webgl.Float32Array

internal fun FloatArray.toFloat32Array(): Float32Array {
    val out = Float32Array(size)
    out.set(toTypedArray())
    return out
}
