package io.github.s52.preslib.esri.importer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsriCustomSymbolMapParserTest {
    @Test
    fun parsesDirectAndFunctionConditions() {
        val file = File.createTempFile("CustomSymbolMap", ".xml")
        file.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <conditionMap name="INT1" alias="INT 1" symbolScale="2.0" version="1.0">
              <feature object="BRIDGE" prim="3">
                <condition functionName="bridge02"/>
              </feature>
              <feature object="BCNLAT,BCNSPP" prim="1">
                <condition symbolName="Q90_Stake_pole.svg">
                  <rule type="A" field="BCNSHP" operator="equal" value="1"/>
                </condition>
              </feature>
            </conditionMap>
            """.trimIndent()
        )

        val map = EsriCustomSymbolMapParser.parse(file)
        assertEquals("INT1", map.name)
        assertEquals(2, map.features.size)
        assertEquals(1, map.directSymbolConditionCount)
        assertEquals(1, map.functionConditionCount)
        assertTrue("BCNLAT" in map.objectNames)
        assertTrue("bridge02" in map.functionNames)
        assertTrue("Q90_Stake_pole.svg" in map.symbolNames)
    }
}
