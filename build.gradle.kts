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


tasks.register("phase5Check") {
    group = "verification"
    description = "Runs Phase 5 critical CSP framework checks and all previous phase checks."
    dependsOn("phase4Check")
}


tasks.register("phase6Check") {
    group = "verification"
    description = "Runs Phase 6 complete CSP coverage checks and all previous phase checks."
    dependsOn("phase5Check")
}


tasks.register("phase7Check") {
    group = "verification"
    description = "Runs Phase 7 draw-command model checks and all previous phase checks."
    dependsOn("phase6Check")
}


tasks.register("phase8Check") {
    group = "verification"
    description = "Runs Phase 8 WebGL2 renderer checks and all previous phase checks."
    dependsOn("phase7Check", ":s52-render-webgl:build", ":demo:build")
}



tasks.register("phase9Check") {
    group = "verification"
    description = "Runs Phase 9 static Presentation Library completeness checks and all previous phase checks."
    dependsOn("phase8Check", ":s52-preslib:jvmTest", ":s52-csp:jvmTest")
}


tasks.register("phase10Check") {
    group = "verification"
    description = "Runs Phase 10 command-level golden portrayal tests and all previous phase checks."
    dependsOn("phase9Check", ":s52-tests:jvmTest")
}

tasks.register("phase11Check") {
    group = "verification"
    description = "Runs Phase 11 S-64 / Chart-1 command validation harness checks and all previous phase checks."
    dependsOn("phase10Check", ":s52-tests:jvmTest")
}
