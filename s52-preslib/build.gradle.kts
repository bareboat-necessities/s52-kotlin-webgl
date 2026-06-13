import org.gradle.process.CommandLineArgumentProvider
import java.util.Base64
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm()
    js(IR) {
        browser()
        binaries.library()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":s52-core"))
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

}

val openCpnRasterAtlasFileNames = listOf(
    "rastersymbols-day.png",
    "rastersymbols-dusk.png",
    "rastersymbols-dark.png"
)

val generateOpenCpnRasterAtlasData = tasks.register("generateOpenCpnRasterAtlasData") {
    group = "generation"
    description = "Embeds OpenCPN raster-symbol PNG atlases into generated commonMain Kotlin data URIs."

    val payloadDir = rootProject.layout.projectDirectory.dir("s52/opencpn")
    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnRasterAtlasData.kt")

    inputs.files(openCpnRasterAtlasFileNames.map { payloadDir.file(it) })
    outputs.file(outputFile)

    doLast {
        fun String.kt(): String = buildString {
            append('"')
            for (ch in this@kt) when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '$' -> { append('\\'); append('$') }
                else -> append(ch)
            }
            append('"')
        }

        fun propertyName(fileName: String): String {
            val parts = fileName
                .removePrefix("rastersymbols-")
                .removeSuffix(".png")
                .replace(Regex("[^A-Za-z0-9]+"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
            if (parts.isEmpty()) return "atlas"
            return parts.first().replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() } +
                parts.drop(1).joinToString(separator = "") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
        }

        val generated = buildString {
            appendLine("package io.github.s52.preslib.opencpn.generated")
            appendLine()
            appendLine("/**")
            appendLine(" * Generated from s52/opencpn/rastersymbols-*.png.")
            appendLine(" *")
            appendLine(" * The browser renderer consumes these data URIs directly, so host")
            appendLine(" * applications do not need to copy rastersymbols-*.png at runtime.")
            appendLine(" */")
            appendLine("object OpenCpnRasterAtlasData {")
            appendLine("    val availableFileNames: Set<String> = setOf(${openCpnRasterAtlasFileNames.joinToString { it.kt() }})")
            appendLine()
            appendLine("    fun dataUriFor(fileName: String): String? = when (fileName) {")
            openCpnRasterAtlasFileNames.forEach { fileName ->
                appendLine("        ${fileName.kt()} -> ${propertyName(fileName)}DataUri")
            }
            appendLine("        else -> null")
            appendLine("    }")

            openCpnRasterAtlasFileNames.forEach { fileName ->
                val file = payloadDir.file(fileName).asFile
                require(file.isFile) { "Missing OpenCPN raster atlas: ${file.absolutePath}" }
                val base64 = Base64.getEncoder().encodeToString(file.readBytes())
                val chunks = base64.chunked(12_000)
                val property = propertyName(fileName)
                appendLine()
                appendLine("    private val ${property}DataUri: String by lazy(LazyThreadSafetyMode.PUBLICATION) {")
                appendLine("        \"data:image/png;base64,\" + ${property}Chunks.joinToString(separator = \"\")")
                appendLine("    }")
                appendLine("    private val ${property}Chunks: Array<String> = arrayOf(")
                chunks.forEachIndexed { index, chunk ->
                    appendLine("        ${chunk.kt()}${if (index == chunks.lastIndex) "" else ","}")
                }
                appendLine("    )")
            }
            appendLine("}")
        }

        outputFile.asFile.parentFile.mkdirs()
        outputFile.asFile.writeText(generated)
    }
}

// Keep generated embedded PNG data fresh for every normal compile/build path.
tasks.matching { task ->
    task.name == "compileKotlinMetadata" ||
        task.name == "compileCommonMainKotlinMetadata" ||
        task.name == "metadataMainClasses" ||
        task.name == "compileKotlinJvm" ||
        task.name == "compileKotlinJs" ||
        task.name == "compileProductionLibraryKotlinJs" ||
        task.name == "compileDevelopmentExecutableKotlinJs" ||
        task.name == "compileProductionExecutableKotlinJs" ||
        task.name == "compileTestKotlinJs"
}.configureEach {
    dependsOn(generateOpenCpnRasterAtlasData)
}


tasks.matching { task ->
    task.name.endsWith("SourcesJar") || task.name == "sourcesJar"
}.configureEach {
    dependsOn(generateOpenCpnRasterAtlasData)
}

tasks.register<JavaExec>("generateOpenCpnRasterAtlasDataWithJvmGenerator") {
    group = "generation"
    description = "Regenerates the embedded OpenCPN raster atlas Kotlin file using the checked-in JVM generator."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.opencpn.generator.OpenCpnRasterAtlasDataGeneratorMain")

    val payloadDir = rootProject.layout.projectDirectory.dir("s52/opencpn")
    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnRasterAtlasData.kt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(payloadDir.asFile.absolutePath, outputFile.asFile.absolutePath)
    })
    inputs.files(openCpnRasterAtlasFileNames.map { payloadDir.file(it) })
    outputs.file(outputFile)
}

tasks.register<JavaExec>("generateSymbologyImages") {
    group = "documentation"
    description = "Generates SVG image artifacts for every known synthetic S-52 symbol, line style, and pattern."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.generator.SymbologyImageGenerator")

    val outputDir = layout.buildDirectory.dir("symbology-images")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(outputDir.get().asFile.absolutePath)
    })
    outputs.dir(outputDir)
}

tasks.register<JavaExec>("openCpnInventory") {
    group = "verification"
    description = "Prints an inventory of the OpenCPN portrayal payload under s52/opencpn."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.opencpn.inventory.OpenCpnInventoryMain")

    argumentProviders.add(CommandLineArgumentProvider {
        listOf(rootProject.layout.projectDirectory.dir("s52/opencpn").asFile.absolutePath)
    })
}

tasks.register<JavaExec>("generateOpenCpnPresLib") {
    group = "generation"
    description = "Regenerates the commonMain OpenCPN Presentation Library pack from s52/opencpn."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.opencpn.generator.OpenCpnPresLibGeneratorMain")

    val payloadDir = rootProject.layout.projectDirectory.dir("s52/opencpn")
    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnGeneratedPresLib.kt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(payloadDir.asFile.absolutePath, outputFile.asFile.absolutePath)
    })
    inputs.file(payloadDir.file("chartsymbols.xml"))
    inputs.files(
        payloadDir.file("rastersymbols-day.png"),
        payloadDir.file("rastersymbols-dusk.png"),
        payloadDir.file("rastersymbols-dark.png")
    )
    outputs.file(outputFile)
}

val esriSourceDirProvider = providers.gradleProperty("esri.sourceDir")
    .orElse(providers.environmentVariable("ESRI_NAUTICAL_CHART_SYMBOLS_DIR"))
    .orElse(rootProject.layout.projectDirectory.dir("s52/esri/source").asFile.absolutePath)

val esriReportDirProvider = layout.buildDirectory.dir("reports/esri")

tasks.register<JavaExec>("esriInventory") {
    group = "verification"
    description = "Inventories ESRI CustomPresentationLibrary XML, Lua, and SVG sources."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.importer.EsriInventoryMain")

    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    outputs.dir(esriReportDirProvider)
}

tasks.register<JavaExec>("validateEsriSvgSubset") {
    group = "verification"
    description = "Validates that ESRI SVG symbols fit the parser subset."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.svg.ValidateEsriSvgSubsetMain")

    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    outputs.dir(esriReportDirProvider)
}

tasks.register<JavaExec>("esriCoverageReport") {
    group = "verification"
    description = "Writes the OpenCPN coverage oracle and initial ESRI direct-rule gap report."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.importer.EsriCoverageReportMain")

    val openCpnGenerated = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnGeneratedPresLib.kt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            openCpnGenerated.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    inputs.file(openCpnGenerated)
    outputs.dir(esriReportDirProvider)
}


tasks.register<JavaExec>("generateEsriVectorSymbols") {
    group = "generation"
    description = "Phase ESRI-3: parses ESRI SVG assets, generates Kotlin vector mesh symbol registry, and writes a generation report."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.generator.GenerateEsriVectorSymbolsMain")

    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedSymbolRegistry.kt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            outputFile.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    outputs.file(outputFile)
    outputs.dir(esriReportDirProvider)
}


tasks.register<JavaExec>("generateEsriDirectRules") {
    group = "generation"
    description = "Phase ESRI-5: generates Kotlin direct/function rule registry from ESRI CustomSymbolMap.xml."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.generator.GenerateEsriDirectRulesMain")

    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedRuleRegistry.kt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            outputFile.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    outputs.file(outputFile)
    outputs.dir(esriReportDirProvider)
}

tasks.register<JavaExec>("checkEsriAliasClosure") {
    group = "verification"
    description = "writes ESRI alias closure reports against OpenCPN symbol candidates and generated ESRI symbols."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses", "generateEsriVectorSymbols")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.alias.EsriAliasClosureReportMain")

    val openCpnGenerated = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnGeneratedPresLib.kt")
    val generatedSymbols = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedSymbolRegistry.kt")
    val aliasDir = rootProject.layout.projectDirectory.dir("s52/esri")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            openCpnGenerated.asFile.absolutePath,
            generatedSymbols.asFile.absolutePath,
            aliasDir.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    inputs.file(openCpnGenerated)
    inputs.file(generatedSymbols)
    inputs.files(
        aliasDir.file("esri-symbol-aliases.tsv"),
        aliasDir.file("esri-line-aliases.tsv"),
        aliasDir.file("esri-pattern-aliases.tsv")
    )
    outputs.dir(esriReportDirProvider)
}


tasks.register<JavaExec>("generateEsriVectorLines") {
    group = "generation"
    description = "parses ESRI line SVG assets and generates Kotlin vector line-style registry."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.generator.GenerateEsriVectorLinesMain")

    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedLineRegistry.kt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            outputFile.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    outputs.file(outputFile)
    outputs.dir(esriReportDirProvider)
}

tasks.register<JavaExec>("generateEsriVectorPatterns") {
    group = "generation"
    description = "Phase ESRI-9: parses ESRI pattern SVG assets and generates Kotlin vector area-pattern registry."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.generator.GenerateEsriVectorPatternsMain")

    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedPatternRegistry.kt")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            outputFile.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    outputs.file(outputFile)
    outputs.dir(esriReportDirProvider)
}


val esriStrictCoverageProvider = providers.gradleProperty("esri.strictCoverage")
    .orElse(providers.environmentVariable("ESRI_STRICT_COVERAGE"))
    .orElse("false")

tasks.register<JavaExec>("generateEsriPresLib") {
    group = "generation"
    description = "Phase ESRI-10: generates the ESRI/INT1 profile metadata facade."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn(
        "jvmMainClasses",
        "generateEsriVectorSymbols",
        "generateEsriVectorLines",
        "generateEsriVectorPatterns",
        "generateEsriDirectRules"
    )
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.generator.GenerateEsriPresLibMain")

    val outputFile = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedPresLib.kt")
    val aliasDir = rootProject.layout.projectDirectory.dir("s52/esri")
    val revisionFile = rootProject.layout.projectDirectory.file("s52/esri/source-revision.properties")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            aliasDir.asFile.absolutePath,
            outputFile.asFile.absolutePath,
            revisionFile.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    inputs.files(
        aliasDir.file("esri-symbol-aliases.tsv"),
        aliasDir.file("esri-line-aliases.tsv"),
        aliasDir.file("esri-pattern-aliases.tsv"),
        revisionFile
    )
    outputs.file(outputFile)
    outputs.dir(esriReportDirProvider)
}

val esriGeneratedKotlinWriterTaskNames = listOf(
    "generateEsriVectorSymbols",
    "generateEsriVectorLines",
    "generateEsriVectorPatterns",
    "generateEsriDirectRules",
    "generateEsriPresLib"
)

// The ESRI generators currently refresh generated registries under src/commonMain/kotlin.
// Gradle 8.14 requires an explicit ordering edge when JS compilation and those generators
// are both in the same task graph. Do NOT apply this to JVM/common metadata compilation:
// the generators themselves run from jvmMainClasses, so ordering JVM compilation after
// the generators creates a circular dependency. The only observed implicit-dependency
// validation failure is from the JS compilation path used by the WebGL build.
tasks.matching { task ->
    task.name == "compileKotlinJs" ||
        task.name == "compileProductionLibraryKotlinJs" ||
        task.name == "compileDevelopmentExecutableKotlinJs" ||
        task.name == "compileProductionExecutableKotlinJs" ||
        task.name == "compileTestKotlinJs"
}.configureEach {
    mustRunAfter(esriGeneratedKotlinWriterTaskNames)
}

tasks.register<JavaExec>("checkEsriStrictCoverage") {
    group = "verification"
    description = "Phase ESRI-11: writes strict ESRI coverage closure reports and optionally fails unresolved release coverage."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn(
        "jvmMainClasses",
        "generateEsriVectorSymbols",
        "generateEsriVectorLines",
        "generateEsriVectorPatterns",
        "generateEsriDirectRules",
        "checkEsriAliasClosure",
        "generateEsriPresLib"
    )
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.coverage.EsriStrictCoverageClosureMain")

    val openCpnGenerated = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/opencpn/generated/OpenCpnGeneratedPresLib.kt")
    val symbolRegistry = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedSymbolRegistry.kt")
    val lineRegistry = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedLineRegistry.kt")
    val patternRegistry = layout.projectDirectory.file("src/commonMain/kotlin/io/github/s52/preslib/esri/generated/EsriGeneratedPatternRegistry.kt")
    val aliasDir = rootProject.layout.projectDirectory.dir("s52/esri")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            openCpnGenerated.asFile.absolutePath,
            symbolRegistry.asFile.absolutePath,
            lineRegistry.asFile.absolutePath,
            patternRegistry.asFile.absolutePath,
            aliasDir.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath,
            esriStrictCoverageProvider.get()
        )
    })
    inputs.files(openCpnGenerated, symbolRegistry, lineRegistry, patternRegistry)
    inputs.files(
        aliasDir.file("esri-symbol-aliases.tsv"),
        aliasDir.file("esri-line-aliases.tsv"),
        aliasDir.file("esri-pattern-aliases.tsv")
    )
    outputs.dir(esriReportDirProvider)
}

tasks.register<JavaExec>("esriNoaaSmokeTest") {
    group = "verification"
    description = "Phase ESRI-12: runs NOAA-style smoke fixtures through the ESRI profile facade and CSP ports."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn("jvmMainClasses", "generateEsriPresLib")
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.smoke.EsriNoaaSmokeMain")

    val fixture = rootProject.layout.projectDirectory.file("s52/esri/noaa-smoke-features.tsv")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            fixture.asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    inputs.file(fixture)
    outputs.dir(esriReportDirProvider)
}


tasks.register<JavaExec>("exportEsriSymbologyImages") {
    group = "documentation"
    description = "Exports OpenCPN-name-compatible ESRI/INT1 symbology artifacts and three point-symbol PNG atlases."

    val jvmCompilation = kotlin.targets.getByName("jvm").compilations.getByName("main")
    dependsOn(
        "jvmMainClasses",
        "generateEsriVectorSymbols",
        "generateEsriVectorLines",
        "generateEsriVectorPatterns"
    )
    classpath = files(jvmCompilation.output.allOutputs, jvmCompilation.runtimeDependencyFiles)
    mainClass.set("io.github.s52.preslib.esri.export.EsriSymbologyImageExportMain")

    val outputDir = layout.buildDirectory.dir("s52-esri-symbology-images")
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            esriSourceDirProvider.get(),
            outputDir.get().asFile.absolutePath,
            esriReportDirProvider.get().asFile.absolutePath
        )
    })
    outputs.dir(outputDir)
    outputs.dir(esriReportDirProvider)
}

tasks.register<Zip>("criticalEsriSymbologyImagesArchive") {
    group = "distribution"
    description = "Archives generated ESRI symbology SVG copies, PNG atlases, manifest, reports, and browser index."
    dependsOn("criticalEsriCheck", "exportEsriSymbologyImages")
    archiveBaseName.set("s52-kotlin-webgl-esri-symbology-images")
    archiveClassifier.set("critical-esri")
    archiveVersion.set(project.version.toString())
    from(layout.buildDirectory.dir("s52-esri-symbology-images")) {
        into("s52-esri-symbology-images")
    }
    from(esriReportDirProvider) {
        into("s52-esri-symbology-images/reports")
    }
}

tasks.register<Zip>("enhancedEsriSvgSetArchive") {
    group = "distribution"
    description = "Archives the OpenCPN-comparable enhanced ESRI SVG portrayal-input set for upload/review before portrayal generation."
    dependsOn("exportEsriSymbologyImages")
    archiveBaseName.set("s52-kotlin-webgl-esri-enhanced-svg-set")
    archiveClassifier.set("portrayal-input")
    archiveVersion.set(project.version.toString())
    from(layout.buildDirectory.dir("s52-esri-symbology-images/enhanced-svg")) {
        into("enhanced-svg")
    }
}

tasks.register("criticalEsriCheck") {
    group = "verification"
    description = "Runs: inventory, SVG validation, vector generation, direct rules, aliases, CSP/profile tests, coverage reports, NOAA smoke, and WebGL build."
    dependsOn(
        "esriInventory",
        "esriCoverageReport",
        "validateEsriSvgSubset",
        "generateEsriVectorSymbols",
        "generateEsriVectorLines",
        "generateEsriVectorPatterns",
        "generateEsriDirectRules",
        "checkEsriAliasClosure",
        "generateEsriPresLib",
        "checkEsriStrictCoverage",
        "esriNoaaSmokeTest",
        "jvmTest",
        ":s52-render-webgl:build"
    )
}
