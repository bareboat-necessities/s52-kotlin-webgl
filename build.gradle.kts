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

tasks.register("phase1Check") {
    group = "verification"
    description = "Runs Phase 1 typed-catalogue validation and all Phase 0 checks."
    dependsOn("phase0Check")
}


tasks.register("phase2Check") {
    group = "verification"
    description = "Runs Phase 2 Presentation Library generation/validation checks and all previous phase checks."
    dependsOn("phase1Check")
}

tasks.register("phase3Check") {
    group = "verification"
    description = "Runs Phase 3 S-52 instruction parser checks and all previous phase checks."
    dependsOn("phase2Check")
}


tasks.register("phase4Check") {
    group = "verification"
    description = "Runs Phase 4 lookup matching, display filtering, and ordering checks plus all previous phase checks."
    dependsOn("phase3Check")
}
