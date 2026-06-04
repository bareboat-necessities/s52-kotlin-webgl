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

tasks.register("phase15ReleaseAudit") {
    group = "verification"
    description = "Checks Phase 15 release-readiness files and safety boundary."

    doLast {
        val requiredFiles = listOf(
            "README.md",
            "CHANGELOG.md",
            "CONTRIBUTING.md",
            "SECURITY.md",
            "docs/PHASES.md",
            "docs/RELEASE_PHASE15.md",
            "samples/integration/minimal-core/README.md",
            ".github/workflows/ci.yml",
            ".github/workflows/release.yml"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 15 release-readiness files: $missing" }

        val readme = layout.projectDirectory.file("README.md").asFile.readText()
        check("Experimental" in readme) { "README.md must keep the experimental safety statement." }
        check("Not type-approved ECDIS" in readme) { "README.md must keep the ECDIS certification boundary." }
        check("Not for navigation" in readme) { "README.md must keep the not-for-navigation boundary." }
    }
}

tasks.register<org.gradle.api.tasks.bundling.Zip>("phase15SourceArchive") {
    group = "distribution"
    description = "Builds a source archive for Phase 15 release handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("source")
    archiveVersion.set(project.version.toString())

    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("phase15Check") {
    group = "verification"
    description = "Runs Phase 15 release-readiness checks and all previous phase checks."
    dependsOn("phase11Check", "phase15ReleaseAudit", ":s52-tests:jvmTest")
}

tasks.register("phase16ApiAudit") {
    group = "verification"
    description = "Checks Phase 16 consumer API facade and integration documentation."

    doLast {
        val requiredFiles = listOf(
            "s52-api/build.gradle.kts",
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52PortrayalSession.kt",
            "docs/API_FACADE_PHASE16.md",
            "samples/integration/facade/README.md"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 16 API facade files: $missing" }

        val readme = layout.projectDirectory.file("README.md").asFile.readText()
        check("s52-api" in readme) { "README.md must document the Phase 16 s52-api facade module." }
        check("phase16Check" in readme) { "README.md must document the Phase 16 check task." }
    }
}

tasks.register<org.gradle.api.tasks.bundling.Zip>("phase16SourceArchive") {
    group = "distribution"
    description = "Builds a source archive for Phase 16 release handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("phase16-source")
    archiveVersion.set(project.version.toString())

    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("phase16Check") {
    group = "verification"
    description = "Runs Phase 16 consumer API facade checks and all previous phase checks."
    dependsOn("phase15Check", "phase16ApiAudit", ":s52-api:build", ":s52-tests:jvmTest")
}

