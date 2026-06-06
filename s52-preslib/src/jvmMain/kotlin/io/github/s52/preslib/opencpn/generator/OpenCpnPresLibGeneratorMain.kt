package io.github.s52.preslib.opencpn.generator

import io.github.s52.preslib.opencpn.inventory.OpenCpnChartSymbolsRawParser
import java.io.File

/** Regenerates OpenCpnGeneratedPresLib.kt from the corrected OpenCPN payload directory. */
object OpenCpnPresLibGeneratorMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val payloadDir = args.getOrNull(0)?.let(::File) ?: File("s52/opencpn")
        val outputFile = args.getOrNull(1)?.let(::File)
            ?: File("s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnGeneratedPresLib.kt")
        val chartsymbols = File(payloadDir, "chartsymbols.xml")
        val summary = OpenCpnChartSymbolsRawParser.parseFile(chartsymbols)
        val source = OpenCpnPresLibSourceConverter.toSourcePack(summary)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(OpenCpnGeneratedKotlinWriter.generate(source))
        println("Wrote ${outputFile.absolutePath}")
        println("lookups=${source.lookupRecords.size} symbols=${source.symbols.size} lineStyles=${source.lineStyles.size} patterns=${source.patterns.size} colorTables=${source.colorTables.size}")
    }
}
