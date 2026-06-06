package io.github.s52.preslib.opencpn.inventory

import java.io.File

/** Parser for OpenCPN's companion S-57 CSV catalog files. */
object OpenCpnCsvCatalogParser {
    fun parseDirectory(directory: File): OpenCpnCsvCatalogSummary = OpenCpnCsvCatalogSummary(
        objectClasses = parseObjectClasses(directory.resolve("s57objectclasses.csv")),
        attributes = parseAttributes(directory.resolve("s57attributes.csv")),
        expectedInputs = parseExpectedInputs(directory.resolve("s57expectedinput.csv")),
        attributeDecodes = parseAttributeDecodes(directory.resolve("attdecode.csv"))
    )

    fun parseObjectClasses(file: File): List<OpenCpnObjectClassRow> = parseCsvIfPresent(file).mapNotNull { row ->
        val code = row["Code"]?.toIntOrNull()
        val objectClass = row["ObjectClass"].orEmpty()
        val acronym = row["Acronym"].orEmpty()
        if (objectClass.isBlank() && acronym.isBlank()) return@mapNotNull null
        OpenCpnObjectClassRow(
            code = code,
            objectClass = objectClass,
            acronym = acronym,
            attributeA = splitSemicolonList(row["Attribute_A"]),
            attributeB = splitSemicolonList(row["Attribute_B"]),
            attributeC = splitSemicolonList(row["Attribute_C"]),
            clazz = row["Class"].orEmpty(),
            primitives = splitSemicolonList(row["Primitives"])
        )
    }

    fun parseAttributes(file: File): List<OpenCpnAttributeRow> = parseCsvIfPresent(file).mapNotNull { row ->
        val attribute = row["Attribute"].orEmpty()
        val acronym = row["Acronym"].orEmpty()
        if (attribute.isBlank() && acronym.isBlank()) return@mapNotNull null
        OpenCpnAttributeRow(
            code = row["Code"]?.toIntOrNull(),
            attribute = attribute,
            acronym = acronym,
            attributeType = row["Attributetype"].orEmpty(),
            clazz = row["Class"].orEmpty()
        )
    }

    fun parseExpectedInputs(file: File): List<OpenCpnExpectedInputRow> = parseCsvIfPresent(file).mapNotNull { row ->
        OpenCpnExpectedInputRow(
            code = row["Code"]?.toIntOrNull(),
            id = row["ID"]?.toIntOrNull(),
            meaning = row["Meaning"].orEmpty()
        ).takeIf { it.code != null || it.id != null || it.meaning.isNotBlank() }
    }

    fun parseAttributeDecodes(file: File): List<OpenCpnAttributeDecodeRow> = parseCsvIfPresent(file).mapNotNull { row ->
        val attribute = row["Attribute"].orEmpty().trim()
        if (attribute.isBlank()) return@mapNotNull null
        OpenCpnAttributeDecodeRow(
            attribute = attribute,
            values = parseDecodeValues(row["ValueDecode"].orEmpty())
        )
    }

    private fun parseCsvIfPresent(file: File): List<Map<String, String>> {
        if (!file.isFile) return emptyList()
        val rows = file.readLines().map { parseCsvLine(it) }
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().trimStart('\ufeff') }
        return rows.drop(1)
            .filter { cells -> cells.any { it.isNotBlank() } }
            .map { cells -> header.indices.associate { index -> header[index] to cells.getOrElse(index) { "" }.trim() } }
    }

    internal fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val ch = line[index]
            when {
                ch == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    cells += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
            index++
        }
        cells += current.toString()
        return cells
    }

    private fun splitSemicolonList(value: String?): List<String> = value.orEmpty()
        .split(';')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    private fun parseDecodeValues(valueDecode: String): List<OpenCpnDecodedAttributeValue> {
        val parts = valueDecode.split(';').map { it.trim() }.filter { it.isNotBlank() }
        val result = mutableListOf<OpenCpnDecodedAttributeValue>()
        var index = 0
        while (index + 1 < parts.size) {
            val id = parts[index].toIntOrNull()
            val label = parts[index + 1]
            if (id != null) result += OpenCpnDecodedAttributeValue(id, label)
            index += 2
        }
        return result
    }
}
