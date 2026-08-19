// This build is consumed two ways and needs a settings file for the second one.
//
// The root build pulls it in with `includeBuild("alohomora-gradle-plugin")` from pluginManagement,
// which works with or without this file — Gradle synthesises one for a single-project included build.
// But a consumer that resolves the plugin from a repository rather than substituting it needs it
// published, and `./gradlew -p alohomora-gradle-plugin publishToMavenLocal` cannot run at all without
// a settings file: Gradle refuses the directory as "not part of the build" defined by the root.
rootProject.name = "alohomora-gradle-plugin"

// Supplies the `alohomora.publish` convention plugin, so the publishing metadata has one home for
// all five published modules instead of a copy here. The root build also includes `build-logic`,
// which makes this a nested inclusion of the same directory — Gradle deduplicates included builds
// by directory, so it resolves to one build, not two.
pluginManagement {
    includeBuild("../build-logic")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
