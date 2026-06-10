package io.github.s52.preslib.esri.profile

import io.github.s52.preslib.esri.csp.EsriCspFeature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsriInt1ProfileTest {
    @Test
    fun exposesMetadataAndCspRegistry() {
        val audit = EsriInt1Profile.audit()
        assertTrue(audit.metadata.name.contains("ESRI"))
        assertTrue(audit.cspCount > 0)
    }

    @Test
    fun cspInstructionsCanBeEvaluatedThroughProfile() {
        val instructions = EsriInt1Profile.cspInstructions(
            "sounding",
            EsriCspFeature("SOUNDG", 1, attributes = mapOf("VALSOU" to "7.2"))
        )
        assertEquals(1, instructions.size)
    }
}
