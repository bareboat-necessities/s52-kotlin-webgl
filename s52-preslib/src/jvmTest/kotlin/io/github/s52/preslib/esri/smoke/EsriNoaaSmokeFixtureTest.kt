package io.github.s52.preslib.esri.smoke

import kotlin.test.Test
import kotlin.test.assertTrue

class EsriNoaaSmokeFixtureTest {
    @Test
    fun bundledSmokeFixtureContainsImportantObjectClasses() {
        val text = java.io.File("../s52/esri/noaa-smoke-features.tsv").takeIf { it.isFile }?.readText()
            ?: java.io.File("s52/esri/noaa-smoke-features.tsv").takeIf { it.isFile }?.readText()
            ?: ""
        assertTrue("SOUNDG" in text)
        assertTrue("WRECKS" in text)
        assertTrue("LIGHTS" in text)
    }
}
