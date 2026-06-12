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
            "DEPARE", "DEPCNT", "SOUNDG", "WRECKS", "OBJL_0", "OBSTRN", "LIGHTS", "TOPMAR",
            "RESARE", "M_QUAL", "M_COVR", "LNDARE", "COALNE", "BOYLAT", "BCNLAT",
            "ACHBRT", "BUAARE", "CBLARE", "CTNARE", "DRYDOC", "HRBFAC", "LNDRGN",
            "PIPARE", "SLOTOP", "UNSARE"
        ).forEach { acronym ->
            assertNotNull(S57ObjectClass.fromAcronym(acronym), "Missing object class $acronym")
        }
    }

    @Test
    fun commonCspAttributesArePresentForFutureProcedures() {
        listOf(
            "DRVAL1", "DRVAL2", "VALSOU", "WATLEV", "CATWRK", "CATOBS", "COLOUR",
            "CATLAM", "LITCHR", "SIGGRP", "SIGPER", "SECTR1", "SECTR2", "CATZOC",
            "CATAIR", "CATLND", "CATSEA", "CATSPM", "CATSIL", "CATSLO", "CATSLC", "NATSUR", "NATQUA", "TRAFIC"
        ).forEach { acronym ->
            assertNotNull(S57Attribute.fromAcronym(acronym), "Missing attribute $acronym")
        }
    }

    @Test
    fun projectLogObjectClassesAndPrimitivesAreAccepted() {
        mapOf(
            S57ObjectClass.ACHARE to listOf(PrimitiveType.Line, PrimitiveType.Area),
            S57ObjectClass.ACHBRT to listOf(PrimitiveType.Point, PrimitiveType.Area),
            S57ObjectClass.BUAARE to listOf(PrimitiveType.Point, PrimitiveType.Area),
            S57ObjectClass.CBLARE to listOf(PrimitiveType.Area),
            S57ObjectClass.CTNARE to listOf(PrimitiveType.Point, PrimitiveType.Area),
            S57ObjectClass.DRYDOC to listOf(PrimitiveType.Area),
            S57ObjectClass.HRBFAC to listOf(PrimitiveType.Point, PrimitiveType.Area),
            S57ObjectClass.LNDRGN to listOf(PrimitiveType.Point, PrimitiveType.Area),
            S57ObjectClass.PIPARE to listOf(PrimitiveType.Point, PrimitiveType.Area),
            S57ObjectClass.SLOTOP to listOf(PrimitiveType.Point, PrimitiveType.Line),
            S57ObjectClass.UNSARE to listOf(PrimitiveType.Area),
            S57ObjectClass.SBDARE to listOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area),
            S57ObjectClass.SLCONS to listOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area),
            S57ObjectClass.MAGVAR to listOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area),
            S57ObjectClass.BUISGL to listOf(PrimitiveType.Point, PrimitiveType.Area),
            S57ObjectClass.LNDARE to listOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area),
            S57ObjectClass.OBJL_0 to listOf(PrimitiveType.Point, PrimitiveType.Line, PrimitiveType.Area)
        ).forEach { (objectClass, primitives) ->
            primitives.forEach { primitive ->
                assertTrue(objectClass.supports(primitive), "${objectClass.acronym} should support $primitive")
            }
        }
    }

    @Test
    fun projectLogAttributesAreKnownToRuntimeCatalogue() {
        listOf(
            S57Attribute.CATAIR,
            S57Attribute.CATLND,
            S57Attribute.CATSEA,
            S57Attribute.CATSPM,
            S57Attribute.CATSIL,
            S57Attribute.CATSLO,
            S57Attribute.CATSLC,
            S57Attribute.NATSUR,
            S57Attribute.NATQUA,
            S57Attribute.TRAFIC
        ).forEach { attribute ->
            assertEquals(attribute, S57Attribute.fromAcronym(attribute.acronym))
        }
        assertEquals(S57AttributeValueKind.EnumerationList, S57Attribute.NATSUR.valueKind)
        assertEquals(S57AttributeValueKind.EnumerationList, S57Attribute.NATQUA.valueKind)
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
