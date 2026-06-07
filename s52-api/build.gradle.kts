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
                api(project(":s52-preslib"))
                api(project(":s52-csp"))
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.register<JavaExec>("exportOpenCpnSymbologyImages") {
    group = "documentation"
    description = "Generates per-asset SVG images from a real imported OpenCPN chartsymbols.xml payload."
    val jvmJarTask = tasks.named<org.gradle.jvm.tasks.Jar>("jvmJar")
    dependsOn(jvmJarTask)
    mainClass.set("io.github.s52.api.tools.S52SymbologyImageExportMainKt")
    classpath(
        files(jvmJarTask.flatMap { it.archiveFile }),
        configurations.named("jvmRuntimeClasspath")
    )

    doFirst {
        val outputDir = rootProject.layout.buildDirectory.dir("s52-symbology-images").get().asFile.absolutePath
        val bundledPlib = rootProject.layout.projectDirectory.file("s52/opencpn/chartsymbols.xml").asFile
        val plibPath = providers.gradleProperty("opencpn.chartsymbols")
            .orElse(providers.environmentVariable("OPENCPN_CHARTSYMBOLS_XML_FILE"))
            .orNull
            ?: bundledPlib.takeIf { it.isFile }?.absolutePath
            ?: error("Missing real OpenCPN chartsymbols.xml payload. Set -Popencpn.chartsymbols=/path/to/chartsymbols.xml, OPENCPN_CHARTSYMBOLS_XML_FILE, or commit s52/opencpn/chartsymbols.xml.")
        args(outputDir, plibPath)
    }
}
