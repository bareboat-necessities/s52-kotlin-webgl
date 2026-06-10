package io.github.s52.preslib.esri.alias

import java.io.File

internal data class EsriAliasRow(
    val sourceName: String,
    val targetName: String,
    val confidence: String,
    val reason: String
)

internal object EsriAliasTableReader {
    fun read(file: File): List<EsriAliasRow> {
        if (!file.isFile) return emptyList()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 2) return@mapNotNull null
                EsriAliasRow(
                    sourceName = parts[0].trim(),
                    targetName = parts[1].trim(),
                    confidence = parts.getOrNull(2)?.trim().orEmpty(),
                    reason = parts.getOrNull(3)?.trim().orEmpty()
                )
            }
    }
}
