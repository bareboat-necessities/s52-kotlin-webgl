package io.github.s52.api

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.settings.S52Palette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S52GalleryTest {
    @Test
    fun s52LibCompatGalleryRendersEveryAvailableAssetKind() {
        val session = S52.s52LibCompat()
        val gallery = session.gallery(S52GalleryRequest(section = S52GallerySection.All))

        assertFalse(session.staticCompletenessReport.hasErrors, session.staticCompletenessReport.toMarkdown())
        assertTrue(gallery.commands.any { it is S52DrawCommand.PointSymbol })
        assertTrue(gallery.commands.any { it is S52DrawCommand.LineComplex })
        assertTrue(gallery.commands.any { it is S52DrawCommand.AreaPattern })
        assertTrue(gallery.commands.any { it is S52DrawCommand.AreaFill })
        assertTrue(gallery.commands.any { it is S52DrawCommand.Text })
    }

    @Test
    fun symbolAndColorGalleriesMatchRegistryCounts() {
        val session = S52.s52LibCompat()
        val symbols = session.gallery(S52GalleryRequest(section = S52GallerySection.Symbols))
        val colors = session.gallery(S52GalleryRequest(section = S52GallerySection.Colors))

        assertEquals(session.presLib.symbols.all().size, symbols.commands.count { it is S52DrawCommand.PointSymbol })
        assertEquals(session.presLib.colors.all(S52Palette.DayBright).size, colors.commands.count { it is S52DrawCommand.AreaFill })
        assertEquals(63, session.presLib.colors.all(S52Palette.DayBright).size)
    }
}
