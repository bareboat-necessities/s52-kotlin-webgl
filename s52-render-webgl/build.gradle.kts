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
            dependencies {
                implementation(project(":s52-core"))
                implementation(project(":s52-preslib"))
            }
        }
    }
}
