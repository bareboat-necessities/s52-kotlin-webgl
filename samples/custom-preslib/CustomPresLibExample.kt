package samples.custompreslib

import io.github.s52.api.S52
import io.github.s52.api.S52Runtime
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack

fun main() {
    val generatedOrLoadedPack: PresLibPack = PresLibPack.synthetic()

    val runtime = S52Runtime.from(
        presLib = generatedOrLoadedPack,
        cspRegistry = DefaultCspRegistry.complete()
    )

    val settings = S52.defaultSettings()
    val report = runtime.performanceReport(
        features = emptyList(),
        settings = settings,
        context = S52.defaultContext(settings)
    )

    println("Runtime ready: ${report.inputFeatureCount} feature(s), ${report.outputCommandCount} command(s).")
}
