rootProject.name = "Alohomora"

pluginManagement {
    repositories {
        google {
            content {
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }

    // Both are plugin-providing builds, so they belong directly under pluginManagement —
    // not nested inside repositories {}, where they only resolved via Kotlin scope fallthrough.
    includeBuild("alohomora-gradle-plugin")
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        mavenCentral()
    }
}
include(":alohomora")
include(":alohomora-common")
include(":alohomora-ui")
include(":alohomora-noop")
include(":showcaseApp")
include(":desktopApp")
