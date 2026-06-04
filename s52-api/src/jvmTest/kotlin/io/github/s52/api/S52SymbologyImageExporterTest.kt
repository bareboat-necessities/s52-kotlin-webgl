package io.github.s52.api

import io.github.s52.api.tools.S52SymbologyImageExporter
import io.github.s52.preslib.PresLibPack
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S52SymbologyImageExporterTest {
    @Test
    fun exporterWritesS52LibCompatImagesNotSyntheticPhase2Pack() {
        val dir = Files.createTempDirectory("s52-symbology-images-test").toFile()
        val report = S52SymbologyImageExporter.exportS52LibCompat(dir)
        val pack = PresLibPack.s52LibCompat()

        assertTrue(dir.resolve("index.html").isFile)
        assertTrue(dir.resolve("manifest.properties").isFile)
        assertTrue(report.symbolCount == pack.symbols.all().size)
        assertTrue(report.lineStyleCount == pack.lineStyles.all().size)
        assertTrue(report.patternCount == pack.patterns.all().size)
        assertTrue(report.colorCount == pack.colors.all(io.github.s52.core.settings.S52Palette.DayBright).size)
        assertTrue(report.fileCount > report.colorCount)

        val manifest = dir.resolve("manifest.properties").readText()
        assertTrue("edition=phase20-s52lib-compat" in manifest)
        assertTrue("synthetic=false" in manifest)
        assertFalse("phase2-synthetic" in manifest)
    }
}
