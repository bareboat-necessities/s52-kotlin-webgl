package io.github.s52.preslib.esri.svg

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class EsriSvgAffine(
    val a: Double = 1.0,
    val b: Double = 0.0,
    val c: Double = 0.0,
    val d: Double = 1.0,
    val e: Double = 0.0,
    val f: Double = 0.0
) {
    fun apply(p: EsriPoint): EsriPoint = EsriPoint(a * p.x + c * p.y + e, b * p.x + d * p.y + f)
    fun then(next: EsriSvgAffine): EsriSvgAffine = EsriSvgAffine(
        a = next.a * a + next.c * b,
        b = next.b * a + next.d * b,
        c = next.a * c + next.c * d,
        d = next.b * c + next.d * d,
        e = next.a * e + next.c * f + next.e,
        f = next.b * e + next.d * f + next.f
    )
}

internal object EsriSvgTransformParser {
    fun parse(raw: String?): EsriSvgAffine {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return EsriSvgAffine()
        var matrix = EsriSvgAffine()
        val regex = Regex("([a-zA-Z]+)\\s*\\(([^)]*)\\)")
        for (match in regex.findAll(text)) {
            val op = match.groupValues[1].lowercase()
            val values = numbers(match.groupValues[2])
            val next = when (op) {
                "matrix" -> if (values.size >= 6) EsriSvgAffine(values[0], values[1], values[2], values[3], values[4], values[5]) else EsriSvgAffine()
                "translate" -> EsriSvgAffine(e = values.getOrElse(0) { 0.0 }, f = values.getOrElse(1) { 0.0 })
                "scale" -> {
                    val sx = values.getOrElse(0) { 1.0 }
                    val sy = values.getOrElse(1) { sx }
                    EsriSvgAffine(a = sx, d = sy)
                }
                "rotate" -> rotate(values)
                else -> EsriSvgAffine()
            }
            matrix = matrix.then(next)
        }
        return matrix
    }

    private fun rotate(values: List<Double>): EsriSvgAffine {
        val angle = values.getOrElse(0) { 0.0 } * PI / 180.0
        val c = cos(angle)
        val s = sin(angle)
        val r = EsriSvgAffine(a = c, b = s, c = -s, d = c)
        if (values.size < 3) return r
        val cx = values[1]
        val cy = values[2]
        return EsriSvgAffine(e = cx, f = cy).then(r).then(EsriSvgAffine(e = -cx, f = -cy))
    }

    private fun numbers(raw: String): List<Double> = Regex("[-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?")
        .findAll(raw)
        .mapNotNull { it.value.toDoubleOrNull() }
        .toList()
}
