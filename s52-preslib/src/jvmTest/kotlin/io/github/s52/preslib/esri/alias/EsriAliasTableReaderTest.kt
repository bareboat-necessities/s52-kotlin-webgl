package io.github.s52.preslib.esri.alias

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class EsriAliasTableReaderTest {
    @Test
    fun readsTabSeparatedAliasRowsSkippingComments() {
        val file = kotlin.io.path.createTempFile().toFile()
        try {
            file.writeText("# comment\nBOYCON01\tQ20b_Conical_buoy.svg\thigh\treason\n")
            val rows = EsriAliasTableReader.read(file)
            assertEquals(1, rows.size)
            assertEquals("BOYCON01", rows.single().sourceName)
            assertEquals("Q20b_Conical_buoy.svg", rows.single().targetName)
        } finally {
            file.delete()
        }
    }
}
