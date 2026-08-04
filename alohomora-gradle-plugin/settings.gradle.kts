// This build is consumed two ways and needs a settings file for the second one.
//
// The root build pulls it in with `includeBuild("alohomora-gradle-plugin")` from pluginManagement,
// which works with or without this file — Gradle synthesises one for a single-project included build.
// But a consumer that resolves the plugin from a repository rather than substituting it needs it
// published, and `./gradlew -p alohomora-gradle-plugin publishToMavenLocal` cannot run at all without
// a settings file: Gradle refuses the directory as "not part of the build" defined by the root.
rootProject.name = "alohomora-gradle-plugin"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
