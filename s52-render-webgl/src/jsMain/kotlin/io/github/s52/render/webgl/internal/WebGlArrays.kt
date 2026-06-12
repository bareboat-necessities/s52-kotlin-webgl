package io.github.s52.render.webgl.internal

import org.khronos.webgl.Float32Array

/**
 * Converts a Kotlin [FloatArray] to a JavaScript [Float32Array] without boxing.
 *
 * The old implementation used `toTypedArray()` and then `Float32Array.set(...)`,
 * which allocates and boxes every vertex component before every WebGL upload.
 * NOAA ENC cells routinely produce thousands of draw commands, so that extra
 * allocation shows up directly as browser jank.  A direct indexed copy keeps the
 * upload path predictable and avoids retaining large boxed arrays for GC.
 */
internal fun FloatArray.toFloat32Array(): Float32Array {
    val out = Float32Array(size)
    for (i in indices) out[i] = this[i]
    return out
}
