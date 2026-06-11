plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        browser()
        binaries.library()
    }

    sourceSets {
        jsMain {
            resources.exclude("s52/opencpn/rastersymbols-*.png")

            dependencies {
                implementation(libs.kotlinx.browser)
                implementation(project(":s52-core"))
                implementation(project(":s52-preslib"))
            }
        }
    }
}
