package io.github.s52.preslib.esri.csp

import kotlin.test.Test
import kotlin.test.assertTrue

class EsriCspPatternTest {
    @Test
    fun restrictedAreaAreaPrimitiveEmitsAreaPattern() {
        val emit = EsriInstructionEmitter()
        val feature = EsriCspFeature(
            acronym = "RESARE",
            primitive = 3,
            listAttributes = mapOf("CATREA" to listOf("7"))
        )
        assertTrue(EsriRestrictedAreaCsp.apply(feature, EsriPortrayalContext(), emit))
        assertTrue(emit.instructions.any { it is EsriInstruction.AreaPattern })
        assertTrue(emit.instructions.any { it is EsriInstruction.ComplexLine })
    }

    @Test
    fun seabedAreaPrimitiveEmitsAreaPatternInsteadOfPointSymbol() {
        val emit = EsriInstructionEmitter()
        val feature = EsriCspFeature(
            acronym = "SBDARE",
            primitive = 3,
            listAttributes = mapOf("NATSUR" to listOf("1"))
        )
        assertTrue(EsriSeabedAreaCsp.apply(feature, EsriPortrayalContext(), emit))
        assertTrue(emit.instructions.any { it is EsriInstruction.AreaPattern })
    }
}
