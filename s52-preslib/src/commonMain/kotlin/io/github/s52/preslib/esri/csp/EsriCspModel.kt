package io.github.s52.preslib.esri.csp

/** Lightweight feature/context model used by the ESRI Lua-to-Kotlin ports. */
data class EsriCspFeature(
    val acronym: String,
    /** 1 point, 2 line, 3 area. */
    val primitive: Int,
    val attributes: Map<String, String> = emptyMap(),
    val listAttributes: Map<String, List<String>> = emptyMap(),
    val leastDepth: Double? = null,
    val greatestDepth: Double? = null,
    val seabedDepth: Double? = null,
    val sweptDepth: Boolean = false,
    val uncertainDepth: Boolean = false,
    val lowAccuracy: Boolean = false,
    val coincidentObjects: Set<String> = emptySet()
) {
    fun string(name: String): String? = attributes[name]
    fun int(name: String): Int? = attributes[name]?.toIntOrNull()
    fun double(name: String): Double? = attributes[name]?.toDoubleOrNull()
    fun list(name: String): List<String> = listAttributes[name] ?: attributes[name]?.let { listOf(it) }.orEmpty()
}

data class EsriPortrayalContext(
    val safetyDepth: Double = 30.0,
    val safetyContour: Double = 30.0,
    val showShallowDangers: Boolean = true,
    val isolatedDangersOff: Boolean = false,
    val shallowPattern: Boolean = true
)

interface EsriConditionalProcedure {
    val names: Set<String>
    fun apply(feature: EsriCspFeature, context: EsriPortrayalContext, emit: EsriInstructionEmitter): Boolean
}

class EsriInstructionEmitter {
    private val mutable = mutableListOf<EsriInstruction>()
    val instructions: List<EsriInstruction> get() = mutable

    fun symbol(name: String, viewingGroup: Int? = null) {
        mutable += EsriInstruction.Symbol(name, viewingGroup)
    }

    fun complexLine(name: String, paint: String? = null, viewingGroup: Int? = null) {
        mutable += EsriInstruction.ComplexLine(name, paint, viewingGroup)
    }

    fun simpleLine(color: String, style: String, width: Double, paint: String? = null) {
        mutable += EsriInstruction.SimpleLine(color, style, width, paint)
    }

    fun areaFill(color: String, transparency: Double = 0.0) {
        mutable += EsriInstruction.AreaFill(color, transparency)
    }

    fun areaPattern(name: String, backgroundColor: String? = null, viewingGroup: Int? = null) {
        mutable += EsriInstruction.AreaPattern(name, backgroundColor, viewingGroup)
    }

    fun text(text: String, color: String? = null, viewingGroup: Int? = null) {
        mutable += EsriInstruction.Text(text, color, viewingGroup)
    }

    fun sounding(depth: Double, color: String = "SNDG1") {
        mutable += EsriInstruction.Sounding(depth, color)
    }
}

sealed interface EsriInstruction {
    data class Symbol(val name: String, val viewingGroup: Int? = null) : EsriInstruction
    data class ComplexLine(val name: String, val paint: String? = null, val viewingGroup: Int? = null) : EsriInstruction
    data class SimpleLine(val color: String, val style: String, val width: Double, val paint: String? = null) : EsriInstruction
    data class AreaFill(val color: String, val transparency: Double = 0.0) : EsriInstruction
    data class AreaPattern(val name: String, val backgroundColor: String? = null, val viewingGroup: Int? = null) : EsriInstruction
    data class Text(val text: String, val color: String? = null, val viewingGroup: Int? = null) : EsriInstruction
    data class Sounding(val depth: Double, val color: String = "SNDG1") : EsriInstruction
}
