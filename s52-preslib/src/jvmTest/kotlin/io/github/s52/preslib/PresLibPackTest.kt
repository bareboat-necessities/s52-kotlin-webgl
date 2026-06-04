package io.github.s52.preslib

import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.settings.S52Palette
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PresLibPackTest {
    @Test
    fun phase0PackContainsSmokeAssets() {
        val pack = PresLibPack.phase0Minimal()
        assertNotNull(pack.colors.color(S52Palette.DayBright, "LANDA"))
        assertNotNull(pack.symbols.find("BOYLAT01"))
        assertTrue(pack.lookupTable.records().any { it.objectClass == S57ObjectClass.DEPARE })
    }
}
