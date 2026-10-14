pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WashItClean"

// Pure-Kotlin game logic. It is a separate build so it can be built and tested
// without the Android SDK (`./gradlew -p core test`).
includeBuild("core")
include(":app")
