pluginManagement {
    repositories {
        /*google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }*/
        google() // 🔥 Sin filtros
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack para librerías de GitHub
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PaltoScan"
include(":app")
include(":core-ml")
