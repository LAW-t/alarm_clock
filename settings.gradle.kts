pluginManagement {
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        } 
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        // JitPack – for third-party Compose libraries (e.g. WheelPickerCompose)
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "alarm_clock"
include(":app")
 