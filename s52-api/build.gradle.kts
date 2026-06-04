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
                api(project(":s52-catalog"))
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
