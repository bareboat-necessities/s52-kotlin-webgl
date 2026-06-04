package io.github.s52.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class S57CatalogTest {
    @Test
    fun objectClassLookupUsesTypedEnum() {
        assertEquals(S57ObjectClass.DEPARE, S57ObjectClass.fromAcronym("depare"))
        assertTrue(S57ObjectClass.WRECKS.supports(PrimitiveType.Point))
        assertTrue(S57ObjectClass.WRECKS.supports(PrimitiveType.Area))
    }

    @Test
    fun attributeLookupUsesTypedEnum() {
        assertEquals(S57Attribute.DRVAL1, S57Attribute.fromAcronym("DRVAL1"))
    }
}
