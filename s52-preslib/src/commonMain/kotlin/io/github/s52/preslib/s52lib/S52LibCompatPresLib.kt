package io.github.s52.preslib.s52lib

import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.generated.GeneratedPresLib
import io.github.s52.preslib.source.PresLibMetadata
import io.github.s52.preslib.source.PresLibPackBuilder
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceColor
import io.github.s52.preslib.source.SourceColorTable

/**
 * Compatibility pack for the public sduclos/S52 libS52 layout.
 *
 * libS52 exposes a Presentation Library manager and OpenGL renderer. The public
 * GitHub tree includes the fallback `S52raz-3.2.rle` color payload and the C
 * rendering/parser code; it does not bundle the restricted official IHO .DAI
 * Presentation Library artwork. This pack therefore imports the public color
 * token set and reuses the project symbol/line/pattern source model so the
 * browser renderer can display every asset present in the loaded pack.
 */
object S52LibCompatPresLib {
    fun sourcePack(): PresLibSourcePack {
        val base = GeneratedPresLib.sourcePack()
        return base.copy(
            metadata = PresLibMetadata(
                name = "s52lib-compatible Presentation Library pack",
                edition = "-s52lib-compat",
                sourceDescription = "Public sduclos/S52-compatible fallback: S52raz color tokens plus all bundled/imported symbols, line styles, patterns, and lookup rows.",
                generatedBy = "S52LibCompatPresLib"
            ),
            colorTables = S52Palette.entries.map { palette -> SourceColorTable(palette, s52LibColors()) }
        )
    }

    fun pack(): PresLibPack = PresLibPackBuilder.build(sourcePack())

    /**
     * Color token list matching libS52's 63 color slots. RGB values are clean-room
     * browser approximations grouped from the public S52raz CIE/L/color-name rows.
     */
    fun s52LibColors(): List<SourceColor> = listOf(
        c("NODTA", 115, 115, 115), c("CURSR", 255, 180, 70), c("CHBLK", 0, 0, 0),
        c("CHGRD", 64, 64, 64), c("CHGRF", 115, 115, 115), c("CHRED", 200, 40, 40),
        c("CHGRN", 65, 170, 75), c("CHYLW", 220, 210, 80), c("CHMGD", 120, 30, 120),
        c("CHMGF", 190, 110, 190), c("CHBRN", 135, 105, 70), c("CHWHT", 255, 255, 255),
        c("SCLBR", 255, 180, 70), c("CHCOR", 255, 180, 70), c("LITRD", 220, 40, 40),
        c("LITGN", 65, 190, 80), c("LITYW", 245, 235, 70), c("ISDNG", 155, 45, 155),
        c("DNGHL", 220, 40, 40), c("TRFCD", 155, 45, 155), c("TRFCF", 210, 130, 210),
        c("LANDA", 210, 190, 135), c("LANDF", 235, 215, 160), c("CSTLN", 70, 70, 70),
        c("SNDG1", 70, 70, 70), c("SNDG2", 0, 0, 0), c("DEPSC", 80, 80, 80),
        c("DEPCN", 70, 70, 70), c("DEPDW", 245, 250, 255), c("DEPMD", 205, 230, 245),
        c("DEPMS", 170, 215, 235), c("DEPVS", 130, 185, 220), c("DEPIT", 180, 195, 110),
        c("RADHI", 65, 190, 80), c("RADLO", 30, 85, 35), c("ARPAT", 60, 140, 150),
        c("NINFO", 255, 180, 70), c("RESBL", 40, 60, 190), c("ADINF", 220, 210, 80),
        c("RESGR", 75, 75, 75), c("SHIPS", 0, 0, 0), c("PSTRK", 0, 0, 0),
        c("SYTRK", 70, 70, 70), c("PLRTE", 170, 20, 20), c("APLRT", 255, 180, 70),
        c("UINFD", 0, 0, 0), c("UINFF", 70, 70, 70), c("UIBCK", 255, 255, 255),
        c("UIAFD", 130, 185, 220), c("UINFR", 220, 40, 40), c("UINFG", 65, 190, 80),
        c("UINFO", 255, 180, 70), c("UINFB", 40, 60, 190), c("UINFM", 155, 45, 155),
        c("UIBDR", 70, 70, 70), c("UIAFF", 210, 190, 135), c("OUTLW", 0, 0, 0),
        c("OUTLL", 70, 70, 70), c("RES01", 170, 40, 170), c("RES02", 40, 60, 190),
        c("RES03", 60, 140, 150), c("BKAJ1", 255, 255, 255), c("BKAJ2", 0, 0, 0)
    )

    private fun c(token: String, r: Int, g: Int, b: Int): SourceColor = SourceColor(token, r, g, b)
}
