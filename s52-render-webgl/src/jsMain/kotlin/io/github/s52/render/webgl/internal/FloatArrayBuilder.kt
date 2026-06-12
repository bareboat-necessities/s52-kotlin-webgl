package io.github.s52.render.webgl.internal

/**
 * Small growable primitive-float buffer for WebGL vertex assembly.
 *
 * Kotlin/JS ArrayList<Float> boxes every coordinate.  That is expensive in the
 * renderer because chart cells can create thousands of short-lived vertex lists
 * per frame.  This builder stores primitive floats and emits one compact
 * FloatArray for the WebGL upload path.
 */
internal class FloatArrayBuilder(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY) {
    private var data: FloatArray = FloatArray(initialCapacity.coerceAtLeast(0))
    var size: Int = 0
        private set

    fun isEmpty(): Boolean = size == 0
    fun isNotEmpty(): Boolean = size != 0

    fun add(value: Float) {
        ensureCapacity(size + 1)
        data[size++] = value
    }

    fun add(x: Float, y: Float) {
        ensureCapacity(size + 2)
        data[size++] = x
        data[size++] = y
    }

    fun add(x: Float, y: Float, u: Float, v: Float) {
        ensureCapacity(size + 4)
        data[size++] = x
        data[size++] = y
        data[size++] = u
        data[size++] = v
    }

    fun addLine(a: ClipPoint, b: ClipPoint) {
        add(a.x, a.y)
        add(b.x, b.y)
    }

    fun addTriangle(a: ClipPoint, b: ClipPoint, c: ClipPoint) {
        add(a.x, a.y)
        add(b.x, b.y)
        add(c.x, c.y)
    }

    fun addTexturedTriangle(
        x0: Float,
        y0: Float,
        u0: Float,
        v0: Float,
        x1: Float,
        y1: Float,
        u1: Float,
        v1: Float,
        x2: Float,
        y2: Float,
        u2: Float,
        v2: Float
    ) {
        add(x0, y0, u0, v0)
        add(x1, y1, u1, v1)
        add(x2, y2, u2, v2)
    }

    fun toFloatArray(): FloatArray = data.copyOf(size)

    private fun ensureCapacity(required: Int) {
        if (required <= data.size) return
        var next = if (data.isEmpty()) DEFAULT_INITIAL_CAPACITY else data.size * 2
        while (next < required) next *= 2
        data = data.copyOf(next)
    }

    private companion object {
        private const val DEFAULT_INITIAL_CAPACITY: Int = 64
    }
}
