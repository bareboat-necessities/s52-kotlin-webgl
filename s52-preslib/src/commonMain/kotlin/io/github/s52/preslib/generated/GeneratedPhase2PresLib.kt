package io.github.s52.preslib.generated

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.source.PresLibMetadata
import io.github.s52.preslib.source.PresLibPackBuilder
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceColor
import io.github.s52.preslib.source.SourceColorTable
import io.github.s52.preslib.source.SourceLineStyle
import io.github.s52.preslib.source.SourceLookupRecord
import io.github.s52.preslib.source.SourcePattern
import io.github.s52.preslib.source.SourceSymbol
import io.github.s52.preslib.source.SourceVectorCommand

/**
 * Generated-style Phase 2 Presentation Library fixture.
 *
 * This is deliberately small and synthetic. It proves the importer, builder,
 * registry, and validation architecture without bundling official IHO assets.
 */
object GeneratedPhase2PresLib {
    fun sourcePack(): PresLibSourcePack = PresLibSourcePack(
        metadata = PresLibMetadata(
            name = "Phase 2 synthetic S-52 Presentation Library pack",
            edition = "phase2-synthetic",
            sourceDescription = "Synthetic data for repository tests. Official IHO Presentation Library assets are not bundled.",
            generatedBy = "GeneratedPhase2PresLib"
        ),
        colorTables = S52Palette.entries.map { palette -> SourceColorTable(palette, colorsFor(palette)) },
        symbols = listOf(
            triangleSymbol("BOYLAT01"),
            diamondSymbol("BOYCAR01"),
            crossSymbol("LIGHTS11"),
            wreckSymbol("WRECKS01")
        ),
        lineStyles = listOf(
            SourceLineStyle("SOLD", "solid line"),
            SourceLineStyle("DASH", "dashed line"),
            SourceLineStyle("COALNE01", "synthetic coastline complex line")
        ),
        patterns = listOf(
            SourcePattern("APACHR01", "synthetic anchorage area pattern")
        ),
        lookupRecords = listOf(
            SourceLookupRecord(
                objectClass = S57ObjectClass.LNDARE,
                primitive = PrimitiveType.Area,
                instruction = "AC(LANDA)",
                displayCategory = DisplayCategory.DisplayBase,
                viewingGroup = 11050,
                displayPriority = 1
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.DEPARE,
                primitive = PrimitiveType.Area,
                instruction = "CS(DEPARE)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 21010,
                displayPriority = 2
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.DEPCNT,
                primitive = PrimitiveType.Line,
                instruction = "LS(SOLD,1,CHGRD)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 21020,
                displayPriority = 3
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.COALNE,
                primitive = PrimitiveType.Line,
                instruction = "LC(COALNE01)",
                displayCategory = DisplayCategory.DisplayBase,
                viewingGroup = 11060,
                displayPriority = 4
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.ACHARE,
                primitive = PrimitiveType.Area,
                instruction = "AP(APACHR01)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 24010,
                displayPriority = 5
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.BOYLAT,
                primitive = PrimitiveType.Point,
                instruction = "SY(BOYLAT01)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 27010,
                displayPriority = 8,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.BOYCAR,
                primitive = PrimitiveType.Point,
                instruction = "SY(BOYCAR01)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 27020,
                displayPriority = 8,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.LIGHTS,
                primitive = PrimitiveType.Point,
                instruction = "SY(LIGHTS11);TX(OBJNAM)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 28010,
                displayPriority = 9,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.WRECKS,
                primitive = PrimitiveType.Point,
                instruction = "SY(WRECKS01)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 34050,
                displayPriority = 8,
                overRadar = true
            )
        )
    )

    fun pack(): PresLibPack = PresLibPackBuilder.build(sourcePack())

    private fun colorsFor(palette: S52Palette): List<SourceColor> {
        val dim = when (palette) {
            S52Palette.DayBright -> 1.00
            S52Palette.DayBlackBack -> 0.92
            S52Palette.DayWhiteBack -> 0.96
            S52Palette.Dusk -> 0.62
            S52Palette.Night -> 0.34
        }

        fun c(token: String, r: Int, g: Int, b: Int): SourceColor = SourceColor(
            token = token,
            r = (r * dim).toInt().coerceIn(0, 255),
            g = (g * dim).toInt().coerceIn(0, 255),
            b = (b * dim).toInt().coerceIn(0, 255)
        )

        return listOf(
            c("DEPVS", 167, 206, 250),
            c("DEPIT", 201, 226, 255),
            c("DEPMS", 214, 236, 255),
            c("DEPDW", 230, 245, 255),
            c("LANDA", 201, 179, 123),
            c("CHBLK", 0, 0, 0),
            c("CHGRD", 110, 110, 110),
            c("SNDG1", 0, 0, 0),
            c("SNDG2", 30, 30, 30),
            c("LITRD", 255, 80, 80),
            c("LITYW", 255, 220, 70)
        )
    }

    private fun triangleSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(8.0, 0.0),
            SourceVectorCommand.LineTo(16.0, 16.0),
            SourceVectorCommand.LineTo(0.0, 16.0),
            SourceVectorCommand.ClosePath
        )
    )

    private fun diamondSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(8.0, 0.0),
            SourceVectorCommand.LineTo(16.0, 8.0),
            SourceVectorCommand.LineTo(8.0, 16.0),
            SourceVectorCommand.LineTo(0.0, 8.0),
            SourceVectorCommand.ClosePath
        )
    )

    private fun crossSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(8.0, 0.0),
            SourceVectorCommand.LineTo(8.0, 16.0),
            SourceVectorCommand.MoveTo(0.0, 8.0),
            SourceVectorCommand.LineTo(16.0, 8.0)
        )
    )

    private fun wreckSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(2.0, 2.0),
            SourceVectorCommand.LineTo(14.0, 14.0),
            SourceVectorCommand.MoveTo(14.0, 2.0),
            SourceVectorCommand.LineTo(2.0, 14.0)
        )
    )
}
