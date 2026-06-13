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
object GeneratedPresLib {
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
            wreckSymbol("WRECKS01"),
            dangerWreckSymbol("WRECKS_DANGER01"),
            obstructionSymbol("OBSTRN01"),
            dangerObstructionSymbol("OBSTRN_DANGER01"),
            triangleSymbol("TOPMAR_CONE_UP01"),
            triangleDownSymbol("TOPMAR_CONE_DOWN01"),
            circleSymbol("TOPMAR_SPHERE01"),
            twoCircleSymbol("TOPMAR_TWO_SPHERES01"),
            rectangleSymbol("TOPMAR_CYLINDER01"),
            crossSymbol("TOPMAR_X01"),
            uprightCrossSymbol("TOPMAR_CROSS01"),
            diamondSymbol("TOPMAR_UNKNOWN01")
        ),
        lineStyles = listOf(
            SourceLineStyle("SOLD", "solid line"),
            SourceLineStyle("DASH", "dashed line"),
            SourceLineStyle("COALNE01", "synthetic coastline complex line"),
            SourceLineStyle("LIGHTSECTOR01", "synthetic light sector line"),
            SourceLineStyle("DATCVR01", "synthetic data-coverage boundary line")
        ),
        patterns = listOf(
            SourcePattern("APACHR01", "synthetic anchorage area pattern"),
            SourcePattern("DANGER01", "synthetic danger highlight pattern"),
            SourcePattern("WRECKS_AREA01", "synthetic wreck area pattern"),
            SourcePattern("OBSTRN_AREA01", "synthetic obstruction area pattern"),
            SourcePattern("RESTRN01", "synthetic restricted-area pattern"),
            SourcePattern("CAUTION01", "synthetic caution-area pattern"),
            SourcePattern("FAIRWY01", "synthetic fairway pattern"),
            SourcePattern("DRGARE01", "synthetic dredged-area pattern"),
            SourcePattern("SBDARE01", "synthetic seabed-area pattern"),
            SourcePattern("MQUAL_LOW01", "synthetic low-quality-data pattern"),
            SourcePattern("MQUAL_GOOD01", "synthetic good-quality-data pattern"),
            SourcePattern("NODATA01", "synthetic no-data pattern")
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
                instruction = "CS(DEPCNT)",
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
                instruction = "CS(ACHARE)",
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
                instruction = "CS(LIGHTS)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 28010,
                displayPriority = 9,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.WRECKS,
                primitive = PrimitiveType.Point,
                instruction = "CS(WRECKS)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 34050,
                displayPriority = 8,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.WRECKS,
                primitive = PrimitiveType.Area,
                instruction = "CS(WRECKS)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 34050,
                displayPriority = 8,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.OBSTRN,
                primitive = PrimitiveType.Point,
                instruction = "CS(OBSTRN)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 34060,
                displayPriority = 8,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.OBSTRN,
                primitive = PrimitiveType.Area,
                instruction = "CS(OBSTRN)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 34060,
                displayPriority = 8,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.SOUNDG,
                primitive = PrimitiveType.Point,
                instruction = "CS(SOUNDG)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 33010,
                displayPriority = 9,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.TOPMAR,
                primitive = PrimitiveType.Point,
                instruction = "CS(TOPMAR)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 27030,
                displayPriority = 9,
                overRadar = true
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.RESARE,
                primitive = PrimitiveType.Area,
                instruction = "CS(RESARE)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 24020,
                displayPriority = 5
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.PRCARE,
                primitive = PrimitiveType.Area,
                instruction = "CS(PRCARE)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 24030,
                displayPriority = 5
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.TESARE,
                primitive = PrimitiveType.Area,
                instruction = "CS(TESARE)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 24040,
                displayPriority = 5
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.FAIRWY,
                primitive = PrimitiveType.Area,
                instruction = "CS(FAIRWY)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 24050,
                displayPriority = 5
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.DRGARE,
                primitive = PrimitiveType.Area,
                instruction = "CS(DRGARE)",
                displayCategory = DisplayCategory.Standard,
                viewingGroup = 21030,
                displayPriority = 3
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.SBDARE,
                primitive = PrimitiveType.Area,
                instruction = "CS(SBDARE)",
                displayCategory = DisplayCategory.Other,
                viewingGroup = 31010,
                displayPriority = 4
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.M_QUAL,
                primitive = PrimitiveType.Area,
                instruction = "CS(M_QUAL)",
                displayCategory = DisplayCategory.Other,
                viewingGroup = 52010,
                displayPriority = 6
            ),
            SourceLookupRecord(
                objectClass = S57ObjectClass.M_COVR,
                primitive = PrimitiveType.Area,
                instruction = "CS(DATCVR)",
                displayCategory = DisplayCategory.DisplayBase,
                viewingGroup = 11010,
                displayPriority = 0
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
            c("DEPSC", 0, 95, 170),
            c("DNGHL", 190, 40, 40),
            c("SNDG1", 0, 0, 0),
            c("SNDG2", 30, 30, 30),
            c("LITRD", 255, 80, 80),
            c("LITYW", 255, 220, 70),
            c("CHMGD", 160, 60, 160),
            c("NODTA", 120, 120, 120),
            c("QUAPOS", 80, 140, 80),
            c("QUASR", 200, 90, 90),
            c("DRGHL", 80, 100, 170)
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


    private fun dangerWreckSymbol(name: String): SourceSymbol = wreckSymbol(name).copy(width = 18.0, height = 18.0)

    private fun obstructionSymbol(name: String): SourceSymbol = diamondSymbol(name)

    private fun dangerObstructionSymbol(name: String): SourceSymbol = crossSymbol(name)

    private fun triangleDownSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(0.0, 0.0),
            SourceVectorCommand.LineTo(16.0, 0.0),
            SourceVectorCommand.LineTo(8.0, 16.0),
            SourceVectorCommand.ClosePath
        )
    )

    private fun circleSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(8.0, 0.0),
            SourceVectorCommand.LineTo(14.0, 4.0),
            SourceVectorCommand.LineTo(14.0, 12.0),
            SourceVectorCommand.LineTo(8.0, 16.0),
            SourceVectorCommand.LineTo(2.0, 12.0),
            SourceVectorCommand.LineTo(2.0, 4.0),
            SourceVectorCommand.ClosePath
        )
    )

    private fun twoCircleSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = circleSymbol(name).commands + listOf(
            SourceVectorCommand.MoveTo(8.0, 4.0),
            SourceVectorCommand.LineTo(8.0, 12.0)
        )
    )

    private fun rectangleSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(4.0, 2.0),
            SourceVectorCommand.LineTo(12.0, 2.0),
            SourceVectorCommand.LineTo(12.0, 14.0),
            SourceVectorCommand.LineTo(4.0, 14.0),
            SourceVectorCommand.ClosePath
        )
    )

    private fun uprightCrossSymbol(name: String): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = 8.0,
        pivotY = 8.0,
        width = 16.0,
        height = 16.0,
        commands = listOf(
            SourceVectorCommand.MoveTo(8.0, 1.0),
            SourceVectorCommand.LineTo(8.0, 15.0),
            SourceVectorCommand.MoveTo(3.0, 8.0),
            SourceVectorCommand.LineTo(13.0, 8.0)
        )
    )

}
