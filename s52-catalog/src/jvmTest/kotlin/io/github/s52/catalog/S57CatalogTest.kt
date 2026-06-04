package io.github.s52.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        assertEquals(S57AttributeValueKind.Decimal, S57Attribute.DRVAL1.valueKind)
        assertEquals(S57AttributeValueKind.EnumerationList, S57Attribute.COLOUR.valueKind)
    }

    @Test
    fun commonS52ObjectClassesArePresentForFutureLookupTables() {
        listOf(
            "DEPARE", "DEPCNT", "SOUNDG", "WRECKS", "OBSTRN", "LIGHTS", "TOPMAR",
            "RESARE", "M_QUAL", "M_COVR", "LNDARE", "COALNE", "BOYLAT", "BCNLAT"
        ).forEach { acronym ->
            assertNotNull(S57ObjectClass.fromAcronym(acronym), "Missing object class $acronym")
        }
    }

    @Test
    fun commonCspAttributesArePresentForFutureProcedures() {
        listOf(
            "DRVAL1", "DRVAL2", "VALSOU", "WATLEV", "CATWRK", "CATOBS", "COLOUR",
            "CATLAM", "LITCHR", "SIGGRP", "SIGPER", "SECTR1", "SECTR2", "CATZOC"
        ).forEach { acronym ->
            assertNotNull(S57Attribute.fromAcronym(acronym), "Missing attribute $acronym")
        }
    }

    @Test
    fun enumeratedValueLookupWorks() {
        assertEquals(
            S57EnumeratedValue.COLOUR_RED,
            S57EnumeratedValue.fromCode(S57Attribute.COLOUR, 3)
        )
        assertEquals(
            S57EnumeratedValue.CATWRK_DANGEROUS,
            S57EnumeratedValue.fromCode(S57Attribute.CATWRK, 2)
        )
    }

    @Test
    fun phase1CatalogValidatesWithoutDuplicateTypedEntries() {
        val report = S57CatalogValidator.validate()

        assertFalse(report.hasErrors, report.diagnostics.joinToString("\n") { it.message })
        assertTrue(report.objectClassCount >= 100)
        assertTrue(report.attributeCount >= 120)
        assertTrue(report.enumeratedValueCount >= 50)
    }
}
