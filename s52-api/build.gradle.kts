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

tasks.register<JavaExec>("exportS52LibSymbologyImages") {
    group = "documentation"
    description = "Generates per-asset SVG images for every asset in the s52lib-compatible S-52 pack."
    val jvmJarTask = tasks.named<org.gradle.jvm.tasks.Jar>("jvmJar")
    dependsOn(jvmJarTask)
    mainClass.set("io.github.s52.api.tools.S52SymbologyImageExportMainKt")
    classpath(
        files(jvmJarTask.flatMap { it.archiveFile }),
        configurations.named("jvmRuntimeClasspath")
    )
    args(rootProject.layout.buildDirectory.dir("s52-symbology-images").get().asFile.absolutePath)
}
