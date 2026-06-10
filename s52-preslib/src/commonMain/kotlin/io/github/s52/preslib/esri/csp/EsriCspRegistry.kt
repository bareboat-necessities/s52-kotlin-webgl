package io.github.s52.preslib.esri.csp

object EsriCspRegistry {
    private val procedures: Map<String, EsriConditionalProcedure> = listOf(
        EsriSoundingCsp,
        EsriLightsCsp,
        EsriWrecks05Csp,
        EsriDepthAreaCsp,
        EsriRestrictedAreaCsp,
        EsriBridgeCsp,
        EsriCableCsp,
        EsriSeabedAreaCsp,
        EsriLandAreaCsp
    ).flatMap { procedure -> procedure.names.map { it.lowercase() to procedure } }.toMap()

    val names: Set<String> = procedures.keys.sorted().toSet()

    fun find(name: String): EsriConditionalProcedure? = procedures[name.lowercase()]

    fun apply(name: String, feature: EsriCspFeature, context: EsriPortrayalContext): List<EsriInstruction> {
        val procedure = find(name) ?: return emptyList()
        val emit = EsriInstructionEmitter()
        procedure.apply(feature, context, emit)
        return emit.instructions
    }
}
