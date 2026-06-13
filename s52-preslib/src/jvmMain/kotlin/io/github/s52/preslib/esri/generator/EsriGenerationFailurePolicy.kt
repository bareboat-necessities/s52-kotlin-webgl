package io.github.s52.preslib.esri.generator

/**
 * Failure policy for ESRI SVG/vector generation.
 *
 * The ESRI CustomPresentationLibrary contains SVG constructs that are broader than
 * the initial ESRI-2/3 parser subset. The normal generation path must be
 * tolerant: generate Kotlin/WebGL data for supported assets, write a complete
 * failure report for unsupported assets, and continue so CI can still build the
 * OpenCPN path plus partial ESRI artifacts.
 *
 * Set one of the strict environment variables below when a release gate must fail
 * on any unsupported SVG asset.
 */
internal object EsriGenerationFailurePolicy {
    fun failOnSvgAssetFailures(): Boolean = envFlag("ESRI_FAIL_ON_SVG_FAILURES") ||
        envFlag("ESRI_STRICT_SVG_GENERATION") ||
        envFlag("ESRI_STRICT_ESRI_GENERATION")

    fun warnPartialGeneration(kind: String, generated: Int, failed: Int, reportPath: String) {
        if (failed <= 0) return
        val message = "Generated $generated ESRI vector $kind; $failed failed. See $reportPath"
        if (failOnSvgAssetFailures()) {
            System.err.println(message)
        } else {
            System.err.println("WARNING: $message")
            System.err.println(
                "WARNING: continuing because ESRI SVG generation is tolerant by default. " +
                    "Set ESRI_FAIL_ON_SVG_FAILURES=true to make this fatal."
            )
        }
    }

    fun warnSubsetValidation(invalidCount: Int, reportPath: String) {
        if (invalidCount <= 0) return
        val message = "ESRI SVG subset validation found $invalidCount unsupported/invalid file(s). See $reportPath"
        if (failOnSvgAssetFailures()) {
            System.err.println(message)
        } else {
            System.err.println("WARNING: $message")
            System.err.println(
                "WARNING: continuing because ESRI-2 validation is report-only by default. " +
                    "Set ESRI_FAIL_ON_SVG_FAILURES=true to make this fatal."
            )
        }
    }

    private fun envFlag(name: String): Boolean {
        val value = System.getenv(name) ?: return false
        return value.equals("true", ignoreCase = true) || value == "1" || value.equals("yes", ignoreCase = true)
    }
}
