import org.gradle.process.CommandLineArgumentProvider

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
    description = "Inventories ESRI CustomPresentationLibrary XML, Lua, and SVG sources for phases ESRI-0/1."

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
    description = "Validates that ESRI SVG symbols fit the phase ESRI-2 parser subset."

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
    description = "Writes the phase ESRI-1 OpenCPN coverage oracle and initial ESRI direct-rule gap report."

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

tasks.register("criticalEsriCheck") {
    group = "verification"
    description = "Runs phases ESRI-0 through ESRI-4 inventory, SVG subset, vector Kotlin generation, and JVM tests."
    dependsOn("esriInventory", "esriCoverageReport", "validateEsriSvgSubset", "generateEsriVectorSymbols", "jvmTest", ":s52-render-webgl:build")
}
