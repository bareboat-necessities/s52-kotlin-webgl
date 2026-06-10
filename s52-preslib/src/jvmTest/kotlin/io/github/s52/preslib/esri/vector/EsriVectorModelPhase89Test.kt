package io.github.s52.preslib.esri.vector

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EsriVectorModelPhase89Test {
    @Test
    fun lineStyleIsRenderableOnlyWithRepeatAndMeshes() {
        val mesh = EsriMesh(
            vertices = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f),
            indices = shortArrayOf(0, 1, 2),
            paint = EsriPaint.Token("CHBLK")
        )
        val line = EsriVectorLineStyle(
            name = "L30_Cable.svg",
            viewBox = EsriSvgViewBox(0f, 0f, 4f, 2f),
            widthMm = 4f,
            heightMm = 2f,
            repeatMm = 4f,
            pivotX = 0f,
            pivotY = 0f,
            meshes = listOf(mesh)
        )
        assertTrue(line.isRenderable)
        assertFalse(line.copy(repeatMm = 0f).isRenderable)
    }

    @Test
    fun areaPatternRequiresPositiveTileAndRenderableMesh() {
        val mesh = EsriMesh(
            vertices = floatArrayOf(0f, 0f, 2f, 0f, 0f, 2f),
            indices = shortArrayOf(0, 1, 2),
            paint = EsriPaint.LiteralHex("#231f20")
        )
        val pattern = EsriVectorAreaPattern(
            name = "J1_Sand.svg",
            viewBox = EsriSvgViewBox(0f, 0f, 2f, 2f),
            tileWidthMm = 2f,
            tileHeightMm = 2f,
            pivotX = 0f,
            pivotY = 0f,
            meshes = listOf(mesh)
        )
        assertTrue(pattern.isRenderable)
        assertFalse(pattern.copy(tileHeightMm = 0f).isRenderable)
    }
}
