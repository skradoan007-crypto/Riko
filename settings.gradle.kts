pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "NexMesh"

include(":app")
include(":core:native")
include(":core:protocol")
include(":core:network")
include(":core:guardian")
include(":core:vdfs")
include(":core:compute")
include(":core:relay")
include(":feature:ui")
