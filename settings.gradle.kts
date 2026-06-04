pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
include(":demo")
