package io.github.s52.preslib.opencpn.generator

import java.io.File

/** Regenerates OpenCpnRasterAtlasData.kt from the OpenCPN raster-symbol PNG atlases. */
object OpenCpnRasterAtlasDataGeneratorMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val payloadDir = args.getOrNull(0)?.let(::File) ?: File("s52/opencpn")
        val outputFile = args.getOrNull(1)?.let(::File)
            ?: File("s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnRasterAtlasData.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(OpenCpnRasterAtlasDataKotlinWriter.generate(payloadDir))
        println("Wrote ${outputFile.absolutePath}")
    }
}
