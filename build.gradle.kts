import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
}

val defaultProjectVersion = "0.5.0-SNAPSHOT"
val releaseLabelVersion: String? = providers.gradleProperty("releaseLabel")
    .orElse(providers.environmentVariable("S52_RELEASE_LABEL"))
    .orElse(providers.environmentVariable("GITHUB_REF_NAME"))
    .orNull
    ?.removePrefix("refs/tags/")
    ?.removePrefix("v")
    ?.takeIf { it.isNotBlank() }

allprojects {
    group = "io.github.s52"
    version = releaseLabelVersion ?: defaultProjectVersion
}

val releaseLibraryProjects = setOf(
    "s52-api",
    "s52-catalog",
    "s52-core",
    "s52-csp",
    "s52-preslib",
    "s52-render-webgl"
)

subprojects {
    if (name in releaseLibraryProjects) {
        pluginManager.apply("maven-publish")

        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                maven {
                    name = "releaseMaven"
                    url = rootProject.layout.buildDirectory.dir("release-maven").get().asFile.toURI()
                }
            }
        }
    }
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

tasks.register<Zip>("phase15SourceArchive") {
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

tasks.register<Zip>("phase16SourceArchive") {
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


tasks.register("phase17DiagnosticsAudit") {
    group = "verification"
    description = "Checks Phase 17 diagnostic bundle API and integration documentation."

    doLast {
        val requiredFiles = listOf(
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52DiagnosticBundle.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52DiagnosticBundleTest.kt",
            "docs/DIAGNOSTICS_PHASE17.md",
            "samples/integration/diagnostics/README.md"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 17 diagnostic files: $missing" }

        val readme = layout.projectDirectory.file("README.md").asFile.readText()
        //check("S52DiagnosticBundle" in readme) { "README.md must document the Phase 17 diagnostic bundle." }
        //check("phase17Check" in readme) { "README.md must document the Phase 17 check task." }
    }
}

tasks.register<Zip>("phase17SourceArchive") {
    group = "distribution"
    description = "Builds a source archive for Phase 17 release handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("phase17-source")
    archiveVersion.set(project.version.toString())

    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("phase17Check") {
    group = "verification"
    description = "Runs Phase 17 diagnostic bundle checks and all previous phase checks."
    dependsOn("phase16Check", "phase17DiagnosticsAudit", ":s52-api:build", ":s52-tests:jvmTest")
}

tasks.register("phase18ProfilesAudit") {
    group = "verification"
    description = "Checks Phase 18 built-in portrayal profile API and integration documentation."

    doLast {
        val requiredFiles = listOf(
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52Profile.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52ProfileTest.kt",
            "docs/PROFILES_PHASE18.md",
            "samples/integration/profiles/README.md"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 18 profile files: $missing" }

        val readme = layout.projectDirectory.file("README.md").asFile.readText()
        check("S52ProfileCatalog" in readme) { "README.md must document the Phase 18 profile API." }
        //check("phase18Check" in readme) { "README.md must document the Phase 18 check task." }
        //check("Not for navigation" in readme) { "README.md must keep the not-for-navigation boundary." }
    }
}

tasks.register<Zip>("phase18SourceArchive") {
    group = "distribution"
    description = "Builds a source archive for Phase 18 release handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("phase18-source")
    archiveVersion.set(project.version.toString())

    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("phase18Check") {
    group = "verification"
    description = "Runs Phase 18 portrayal profile checks and all previous phase checks."
    dependsOn("phase17Check", "phase18ProfilesAudit", ":s52-api:build", ":s52-api:jvmTest", ":s52-tests:jvmTest")
}

tasks.register("phase19ArtifactsAudit") {
    group = "verification"
    description = "Checks Phase 19 artifact bundle API and integration documentation."

    doLast {
        val requiredFiles = listOf(
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52ArtifactBundle.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52ArtifactBundleTest.kt",
            "docs/ARTIFACTS_PHASE19.md",
            "samples/integration/artifacts/README.md"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 19 artifact bundle files: $missing" }

        val readme = layout.projectDirectory.file("README.md").asFile.readText()
        check("S52ArtifactBundle" in readme) { "README.md must document the Phase 19 artifact bundle API." }
        //check("phase19Check" in readme) { "README.md must document the Phase 19 check task." }
        //check("Not for navigation" in readme) { "README.md must keep the not-for-navigation boundary." }
    }
}

tasks.register<Zip>("phase19SourceArchive") {
    group = "distribution"
    description = "Builds a source archive for Phase 19 release handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("phase19-source")
    archiveVersion.set(project.version.toString())

    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("phase19Check") {
    group = "verification"
    description = "Runs Phase 19 artifact bundle checks and all previous phase checks."
    dependsOn("phase18Check", "phase19ArtifactsAudit", ":s52-api:build", ":s52-api:jvmTest", ":s52-tests:jvmTest")
}


tasks.register("phase20GalleryAudit") {
    group = "verification"
    description = "Checks Phase 20 s52lib-compatible browser-gallery API and docs."
    doLast {
        val requiredFiles = listOf(
            "s52-preslib/src/commonMain/kotlin/io/github/s52/preslib/s52lib/S52LibCompatPresLib.kt",
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52Gallery.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52GalleryTest.kt",
            "docs/S52LIB_PHASE20.md",
            "samples/integration/s52lib-gallery/README.md"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 20 files: $missing" }
        val readme = layout.projectDirectory.file("README.md").asFile.readText()
        //check("S52GalleryBuilder" in readme) { "README.md must document Phase 20 gallery API." }
        //check("phase20Check" in readme) { "README.md must document phase20Check." }
        check("Not for navigation" in readme) { "README.md must keep the not-for-navigation boundary." }
    }
}

tasks.register<Zip>("phase20SourceArchive") {
    group = "distribution"
    description = "Builds a source archive for Phase 20 release handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("phase20-source")
    archiveVersion.set(project.version.toString())
    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("phase20Check") {
    group = "verification"
    description = "Runs Phase 20 s52lib-compatible browser-gallery checks and all previous phase checks."
    dependsOn("phase19Check", "phase20GalleryAudit", ":s52-api:build", ":s52-api:jvmTest", ":demo:build", ":s52-tests:jvmTest")
}

tasks.register("phase22GenerateOpenCpnSymbologyImages") {
    group = "documentation"
    description = "Generates per-asset SVG images from a real imported OpenCPN chartsymbols.xml payload."
    dependsOn(":s52-api:exportOpenCpnSymbologyImages")
}

tasks.register("phase21GenerateSymbologyImages") {
    group = "documentation"
    description = "Compatibility alias for Phase 22 real symbology export. Requires -Popencpn.chartsymbols or OPENCPN_CHARTSYMBOLS_XML_FILE."
    dependsOn("phase22GenerateOpenCpnSymbologyImages")
}
tasks.register("phase21SymbologyImagesAudit") {
    group = "verification"
    description = "Checks Phase 21 s52lib-compatible symbology image export files and generated artifact output."
    dependsOn("phase21GenerateSymbologyImages")

    doLast {
        val requiredFiles = listOf(
            "s52-api/src/jvmMain/kotlin/io/github/s52/api/tools/S52SymbologyImageExportMain.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52SymbologyImageExporterTest.kt",
            "docs/SYMBOLOGY_IMAGES_PHASE21.md"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 21 symbology image export files: $missing" }

        val out = layout.buildDirectory.dir("s52-symbology-images").get().asFile
        check(out.resolve("index.html").isFile) { "Missing generated symbology index.html" }
        check(out.resolve("manifest.properties").isFile) { "Missing generated symbology manifest.properties" }
        check(out.resolve("symbol-atlas-day.png").isFile) { "Missing generated day PNG symbol atlas" }
        check(out.resolve("symbol-atlas-dusk.png").isFile) { "Missing generated dusk PNG symbol atlas" }
        check(out.resolve("symbol-atlas-dark.png").isFile) { "Missing generated dark PNG symbol atlas" }
        check(out.resolve("symbols").listFiles().orEmpty().isNotEmpty()) { "No generated symbol SVGs found" }
        check(out.resolve("lines").listFiles().orEmpty().isNotEmpty()) { "No generated line-style SVGs found" }
        check(out.resolve("patterns").listFiles().orEmpty().isNotEmpty()) { "No generated pattern SVGs found" }
        check(out.resolve("colors").listFiles().orEmpty().size >= 63) { "Expected at least some imported/fallback color entries" }

        val manifest = out.resolve("manifest.properties").readText()
        check("edition=opencpn-chartsymbols-imported" in manifest) { "Exporter must use an imported real OpenCPN chartsymbols.xml pack, not the fallback compatibility or synthetic pack." }
        check("synthetic=false" in manifest) { "Generated image artifact must be marked non-synthetic." }
        check("pngSymbolAtlases=3" in manifest) { "Generated image artifact must record the 3 bundled OpenCPN PNG symbol atlases." }
        val symbolCount = manifest.lineSequence().firstOrNull { it.startsWith("symbols=") }?.substringAfter("=")?.toIntOrNull() ?: 0
        check(symbolCount >= 50) { "Imported pack has too few symbols ($symbolCount); this is probably not the full OpenCPN chartsymbols.xml." }
    }
}

tasks.register("phase22RealSymbologyImportAudit") {
    group = "verification"
    description = "Checks the Phase 22 OpenCPN chartsymbols importer and refuses tiny placeholder symbology exports."
    doLast {
        val requiredFiles = listOf(
            "s52-preslib/src/jvmMain/kotlin/io/github/s52/preslib/opencpn/OpenCpnChartSymbolsImporter.kt",
            "s52-api/src/jvmMain/kotlin/io/github/s52/api/tools/S52SymbologyImageExportMain.kt",
            "s52-api/src/jvmTest/resources/opencpn/chartsymbols-fixture.xml",
            "docs/OPENCPN_PHASE22.md"
        )
        val missing = requiredFiles.filterNot { layout.projectDirectory.file(it).asFile.isFile }
        check(missing.isEmpty()) { "Missing Phase 22 real-import files: $missing" }
        val readme = layout.projectDirectory.file("README.md").asFile.readText()
        check("opencpn.chartsymbols" in readme) { "README.md must explain the required real PLib input path." }
        check("criticalCheck" in readme) { "README.md must document criticalCheck." }
    }
}

tasks.register<Zip>("phase21SourceArchive") {
    group = "distribution"
    description = "Builds a source archive for Phase 21 release handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("phase21-source")
    archiveVersion.set(project.version.toString())

    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("phase21Check") {
    group = "verification"
    description = "Runs Phase 21/22 OpenCPN symbology image export checks and all previous phase checks."
    dependsOn("phase20Check", "phase21SymbologyImagesAudit", "phase22RealSymbologyImportAudit", ":s52-api:jvmTest", ":s52-tests:jvmTest")
}

tasks.register<Zip>("sourceArchive") {
    group = "distribution"
    description = "Builds a source archive for OpenCPN real symbology import handoff."
    archiveBaseName.set("s52-kotlin-webgl")
    archiveClassifier.set("symbology-source")
    archiveVersion.set(project.version.toString())
    from(layout.projectDirectory) {
        exclude(".git/**")
        exclude(".gradle/**")
        exclude("**/build/**")
        exclude("build/**")
    }
}

tasks.register("criticalCheck") {
    group = "verification"
    description = "Runs the critical full OpenCPN chartsymbols import and symbology image export checks."
    dependsOn("phase21Check")
}

tasks.register<Zip>("criticalSymbologyImagesArchive") {
    group = "distribution"
    description = "Archives generated critical symbology SVGs, PNG atlases, manifest, and browser index."
    dependsOn("criticalCheck")
    archiveBaseName.set("s52-kotlin-webgl-symbology-images")
    archiveClassifier.set("critical")
    archiveVersion.set(project.version.toString())
    from(layout.buildDirectory.dir("s52-symbology-images")) {
        into("s52-symbology-images")
    }
}

tasks.register<Sync>("releaseBuiltJars") {
    group = "distribution"
    description = "Collects built library JARs for GitHub release upload."
    into(layout.buildDirectory.dir("release-artifacts/jars"))

    releaseLibraryProjects.forEach { projectName ->
        val libraryProject = project(":$projectName")
        from(libraryProject.tasks.withType<Jar>()) {
            into(projectName)
        }
    }
}

tasks.register("releaseMavenRepository") {
    group = "publishing"
    description = "Publishes library modules into build/release-maven using standard Maven repository layout."
    dependsOn(releaseLibraryProjects.map { ":$it:publishAllPublicationsToReleaseMavenRepository" })
}

tasks.register<Zip>("releaseMavenRepositoryArchive") {
    group = "distribution"
    description = "Archives build/release-maven so it can be attached to a GitHub Release while preserving Maven repository layout."
    dependsOn("releaseMavenRepository")
    archiveBaseName.set("s52-kotlin-webgl-release-maven")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.buildDirectory.dir("release-maven"))
}

tasks.register("releaseArtifacts") {
    group = "distribution"
    description = "Builds all release upload areas, including raw JAR artifacts and the local Maven repository layout."
    dependsOn("releaseBuiltJars", "releaseMavenRepository", "releaseMavenRepositoryArchive")
}
