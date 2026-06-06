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

