plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
}

allprojects {
    group = "io.github.s52"
    version = "0.1.0-SNAPSHOT"
}

tasks.register("phase0Check") {
    group = "verification"
    description = "Runs the Phase 0 build, JVM smoke tests, and demo webpack build."
    dependsOn(
        ":s52-catalog:build",
        ":s52-core:build",
        ":s52-preslib:build",
        ":s52-csp:build",
        ":s52-render-webgl:build",
        ":demo:build"
    )
}
