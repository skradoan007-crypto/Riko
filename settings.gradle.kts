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

rootProject.name = "VyRox"

include(":app")
include(":core:model")
include(":core:common")
include(":core:design")
include(":core:db")
include(":core:storage")
include(":domain")
include(":data")
include(":graphics:gl")
include(":media:engine")
include(":feature:home")
include(":feature:color")
include(":feature:editor")
include(":feature:export")
