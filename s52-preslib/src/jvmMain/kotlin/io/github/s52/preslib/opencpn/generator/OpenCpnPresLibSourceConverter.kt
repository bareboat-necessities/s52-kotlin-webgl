package io.github.s52.preslib.opencpn.generator

import io.github.s52.catalog.S57ObjectClassKey
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.opencpn.inventory.OpenCpnChartSymbolsSummary
import io.github.s52.preslib.opencpn.inventory.OpenCpnDisplayCategory
import io.github.s52.preslib.opencpn.inventory.OpenCpnRadarPriority
import io.github.s52.preslib.opencpn.inventory.OpenCpnRawAsset
import io.github.s52.preslib.opencpn.inventory.OpenCpnRawBitmap
import io.github.s52.preslib.opencpn.inventory.OpenCpnRawLookupRecord
import io.github.s52.preslib.source.PresLibMetadata
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceAttributeFilter
import io.github.s52.preslib.source.SourceBitmapRef
import io.github.s52.preslib.source.SourceColor
import io.github.s52.preslib.source.SourceColorTable
import io.github.s52.preslib.source.SourceLineStyle
import io.github.s52.preslib.source.SourceLookupRecord
import io.github.s52.preslib.source.SourcePattern
import io.github.s52.preslib.source.SourceSymbol

/** Converts Phase 28A/28B raw OpenCPN inventory records into the common source-pack model. */
object OpenCpnPresLibSourceConverter {
    fun toSourcePack(summary: OpenCpnChartSymbolsSummary): PresLibSourcePack = PresLibSourcePack(
        metadata = PresLibMetadata(
            name = "OpenCPN S-52 Presentation Library Pack",
            edition = "opencpn-chartsymbols-xml",
            sourceDescription = "Generated from corrected baseline s52/opencpn payload: chartsymbols.xml plus raster atlases.",
            generatedBy = "OpenCpnPresLibGenerator"
        ),
        colorTables = summary.colorTables.map { table ->
            SourceColorTable(
                palette = table.name.toPalette(),
                colors = table.colors.map { SourceColor(it.name, it.r, it.g, it.b) }
            )
        },
        symbols = summary.symbols.map { it.toSourceSymbol() },
        lineStyles = summary.lineStyles.map { it.toSourceLineStyle() },
        patterns = summary.patterns.map { it.toSourcePattern() },
        lookupRecords = summary.lookups.map { it.toSourceLookupRecord() }
    )

    private fun OpenCpnRawAsset.toSourceSymbol(): SourceSymbol = SourceSymbol(
        name = name,
        pivotX = vector?.pivot?.x ?: bitmap?.pivot?.x ?: 0.0,
        pivotY = vector?.pivot?.y ?: bitmap?.pivot?.y ?: 0.0,
        width = vector?.width ?: bitmap?.width?.toDouble() ?: 0.0,
        height = vector?.height ?: bitmap?.height?.toDouble() ?: 0.0,
        colorRefs = colorRefs,
        bitmap = bitmap?.toSourceBitmapRef(),
        vectorHpgl = vector?.hpgl?.takeIf { it.isNotBlank() }
    )

    private fun OpenCpnRawAsset.toSourceLineStyle(): SourceLineStyle = SourceLineStyle(
        name = name,
        description = description,
        colorRefs = colorRefs,
        bitmap = bitmap?.toSourceBitmapRef(),
        vectorHpgl = vector?.hpgl?.takeIf { it.isNotBlank() }
    )

    private fun OpenCpnRawAsset.toSourcePattern(): SourcePattern = SourcePattern(
        name = name,
        description = description,
        colorRefs = colorRefs,
        bitmap = bitmap?.toSourceBitmapRef(),
        vectorHpgl = vector?.hpgl?.takeIf { it.isNotBlank() }
    )

    private fun OpenCpnRawBitmap.toSourceBitmapRef(): SourceBitmapRef? {
        val location = graphicsLocation ?: return null
        return SourceBitmapRef(
            atlasFileName = "rastersymbols-day.png",
            x = location.x,
            y = location.y,
            width = width?.toDouble() ?: 0.0,
            height = height?.toDouble() ?: 0.0,
            pivotX = pivot?.x ?: 0.0,
            pivotY = pivot?.y ?: 0.0,
            originX = origin?.x ?: 0.0,
            originY = origin?.y ?: 0.0
        )
    }

    private fun OpenCpnRawLookupRecord.toSourceLookupRecord(): SourceLookupRecord {
        val key = objectClassKey
        return SourceLookupRecord(
            objectClass = key.standard,
            objectClassKey = key,
            primitive = primitive,
            instruction = instruction,
            displayCategory = displayCategory.toRuntime(),
            viewingGroup = viewingGroup ?: 0,
            displayPriority = displayPriority,
            overRadar = radarPriority == OpenCpnRadarPriority.OverRadar,
            attributeFilter = SourceAttributeFilter.Any,
            sourceTableName = tableName.name,
            sourceDisplayPriorityLabel = displayPriorityLabel,
            sourceRadarPriority = radarPriority.name,
            rawAttribCodes = attribCodes
        )
    }

    private fun String.toPalette(): S52Palette = when (trim().uppercase()) {
        "DAY_BRIGHT" -> S52Palette.DayBright
        "DAY_BLACKBACK" -> S52Palette.DayBlackBack
        "DAY_WHITEBACK" -> S52Palette.DayWhiteBack
        "DUSK" -> S52Palette.Dusk
        "NIGHT" -> S52Palette.Night
        else -> error("Unsupported OpenCPN color table '$this'")
    }

    private fun OpenCpnDisplayCategory.toRuntime(): DisplayCategory = when (this) {
        OpenCpnDisplayCategory.DisplayBase -> DisplayCategory.DisplayBase
        OpenCpnDisplayCategory.Standard -> DisplayCategory.Standard
        OpenCpnDisplayCategory.Other -> DisplayCategory.Other
        OpenCpnDisplayCategory.Mariners -> DisplayCategory.MarinersStandard
        OpenCpnDisplayCategory.Unknown -> DisplayCategory.Other
    }
}
