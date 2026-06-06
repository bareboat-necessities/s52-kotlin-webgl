package io.github.s52.preslib.opencpn.inventory

import java.io.File

/** Small JVM tool for verifying the OpenCPN payload shipped with this repository. */
object OpenCpnInventoryMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val directory = if (args.isNotEmpty()) File(args[0]) else findDefaultOpenCpnDirectory()
        val inventory = OpenCpnPayloadInventoryReader.read(directory)
        print(inventory.toHumanText())
        if (inventory.diagnostics.hasIssues()) {
            throw IllegalStateException("OpenCPN portrayal payload has diagnostics; see output above.")
        }
    }

    private fun findDefaultOpenCpnDirectory(): File {
        val start = File(System.getProperty("user.dir")).absoluteFile
        generateSequence(start) { it.parentFile }.forEach { dir ->
            val candidate = dir.resolve("s52/opencpn")
            if (candidate.isDirectory) return candidate
        }
        return start.resolve("s52/opencpn")
    }
}
