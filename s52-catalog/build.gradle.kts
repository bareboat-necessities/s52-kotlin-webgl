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
        commonMain {}
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
