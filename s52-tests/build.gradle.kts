plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm()

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
                implementation(project(":s52-api"))
            }
        }
    }
}
