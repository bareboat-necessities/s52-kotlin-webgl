plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        jsMain {
            resources.exclude("s52/opencpn/rastersymbols-*.png")

            dependencies {
                implementation(libs.kotlinx.browser)
                implementation(project(":s52-catalog"))
                implementation(project(":s52-core"))
                implementation(project(":s52-preslib"))
                implementation(project(":s52-csp"))
                implementation(project(":s52-api"))
                implementation(project(":s52-render-webgl"))
            }
        }
    }
}
