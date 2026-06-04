pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "s52-kotlin-webgl"

include(":s52-catalog")
include(":s52-core")
include(":s52-preslib")
include(":s52-csp")
include(":s52-render-webgl")
include(":s52-api")
include(":demo")
include(":s52-tests")
