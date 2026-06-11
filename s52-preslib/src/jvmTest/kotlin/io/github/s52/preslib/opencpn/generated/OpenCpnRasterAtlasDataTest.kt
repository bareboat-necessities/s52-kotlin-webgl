package io.github.s52.preslib.opencpn.generated

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.util.Base64

class OpenCpnRasterAtlasDataTest {
    @Test
    fun embedsAllOpenCpnRasterAtlasesAsPngDataUris() {
        assertEquals(
            setOf("rastersymbols-day.png", "rastersymbols-dusk.png", "rastersymbols-dark.png"),
            OpenCpnRasterAtlasData.availableFileNames
        )

        OpenCpnRasterAtlasData.availableFileNames.forEach { fileName ->
            val dataUri = assertNotNull(OpenCpnRasterAtlasData.dataUriFor(fileName), "missing embedded atlas $fileName")
            assertTrue(dataUri.startsWith("data:image/png;base64,"), "atlas must be embedded as a PNG data URI")
            val bytes = Base64.getDecoder().decode(dataUri.substringAfter(','))
            assertContentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a),
                bytes.take(8).toByteArray(),
                "embedded atlas must decode to a PNG file"
            )
        }
    }
}
