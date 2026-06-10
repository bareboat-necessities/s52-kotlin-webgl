package io.github.s52.preslib.esri.generator

import io.github.s52.preslib.esri.svg.EsriSvgMeshGenerator
import io.github.s52.preslib.esri.svg.EsriSvgParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class EsriSvgMeshGeneratorTest {
    @Test
    fun generatesFilledTriangleMesh() {
        val file = tempSvg("""
            <svg width="1mm" height="1mm" viewBox="0 0 10 10" xmlns="http://www.w3.org/2000/svg">
              <path d="M 0 0 L 10 0 L 0 10 Z" fill="#231f20"/>
            </svg>
        """.trimIndent())
        val doc = EsriSvgParser.parse(file, "POINT")
        val meshes = EsriSvgMeshGenerator.generate(doc)
        assertEquals(1, meshes.size)
        assertTrue(meshes.single().vertexCount >= 3)
        assertTrue(meshes.single().triangleCount >= 1)
    }

    @Test
    fun generatesStrokeMesh() {
        val file = tempSvg("""
            <svg width="1mm" height="1mm" viewBox="0 0 10 10" xmlns="http://www.w3.org/2000/svg">
              <path d="M 0 0 L 10 0" fill="none" stroke="#231f20" stroke-width="1"/>
            </svg>
        """.trimIndent())
        val doc = EsriSvgParser.parse(file, "LINE")
        val meshes = EsriSvgMeshGenerator.generate(doc)
        assertEquals(1, meshes.size)
        assertEquals(4, meshes.single().vertexCount)
        assertEquals(2, meshes.single().triangleCount)
    }

    private fun tempSvg(text: String): File = createTempFile(prefix = "esri-test", suffix = ".svg").apply {
        writeText(text)
        deleteOnExit()
    }
}
