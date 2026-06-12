package io.github.s52.render.webgl.internal

import org.khronos.webgl.Float32Array

/**
 * Converts a Kotlin [FloatArray] to a JavaScript [Float32Array] without boxing.
 *
 * Do not use `Float32Array.set(index, value)` or `out[index] = value` here:
 * the Kotlin/JS WebGL typed-array bindings expose only bulk `set(array, offset)`
 * overloads, not an indexed Kotlin operator setter.  Also avoid `asDynamic()` in
 * this hot path because previous production bundles exposed a minified runtime
 * failure around dynamic member calls.  A tiny JS indexed write compiles to the
 * native typed-array assignment we need.
 */
internal fun FloatArray.toFloat32Array(): Float32Array {
    val out = Float32Array(size)
    for (i in indices) {
        writeFloat32(out, i, this[i])
    }
    return out
}

private fun writeFloat32(array: Float32Array, index: Int, value: Float) {
    js("array[index] = value")
}
