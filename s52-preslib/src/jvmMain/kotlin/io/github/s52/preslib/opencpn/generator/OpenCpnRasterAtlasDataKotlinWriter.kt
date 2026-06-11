package io.github.s52.preslib.opencpn.generator

import java.io.File
import java.util.Base64

/** Generates commonMain Kotlin that embeds OpenCPN raster-symbol PNG atlases as base64 data URIs. */
object OpenCpnRasterAtlasDataKotlinWriter {
    private val defaultAtlasFileNames = listOf(
        "rastersymbols-day.png",
        "rastersymbols-dusk.png",
        "rastersymbols-dark.png"
    )

    fun generate(payloadDir: File, atlasFileNames: List<String> = defaultAtlasFileNames): String = buildString {
        appendLine("package io.github.s52.preslib.opencpn.generated")
        appendLine()
        appendLine("/**")
        appendLine(" * Generated from s52/opencpn/rastersymbols-*.png.")
        appendLine(" *")
        appendLine(" * The browser renderer consumes these data URIs directly, so host")
        appendLine(" * applications do not need to copy rastersymbols-*.png at runtime.")
        appendLine(" */")
        appendLine("object OpenCpnRasterAtlasData {")
        appendLine("    val availableFileNames: Set<String> = setOf(${atlasFileNames.joinToString { it.kt() }})")
        appendLine()
        appendLine("    fun dataUriFor(fileName: String): String? = when (fileName) {")
        atlasFileNames.forEach { fileName -> appendLine("        ${fileName.kt()} -> ${propertyName(fileName)}DataUri") }
        appendLine("        else -> null")
        appendLine("    }")
        appendLine()
        atlasFileNames.forEachIndexed { index, fileName ->
            if (index > 0) appendLine()
            val file = payloadDir.resolve(fileName)
            require(file.isFile) { "Missing OpenCPN raster atlas: ${file.absolutePath}" }
            val base64 = Base64.getEncoder().encodeToString(file.readBytes())
            val chunks = base64.chunked(12_000)
            val property = propertyName(fileName)
            appendLine("    private val ${property}DataUri: String by lazy(LazyThreadSafetyMode.PUBLICATION) {")
            appendLine("        \"data:image/png;base64,\" + ${property}Chunks.joinToString(separator = \"\")")
            appendLine("    }")
            appendLine("    private val ${property}Chunks: Array<String> = arrayOf(")
            chunks.forEachIndexed { chunkIndex, chunk ->
                appendLine("        ${chunk.kt()}${if (chunkIndex == chunks.lastIndex) "" else ","}")
            }
            appendLine("    )")
        }
        appendLine("}")
    }

    private fun propertyName(fileName: String): String = fileName
        .removePrefix("rastersymbols-")
        .removeSuffix(".png")
        .replace(Regex("[^A-Za-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(separator = "") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        .replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() }
        .ifBlank { "atlas" }

    private fun String.kt(): String = buildString {
        append('"')
        for (ch in this@kt) when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '$' -> { append('\\'); append('$') }
            else -> append(ch)
        }
        append('"')
    }
}
