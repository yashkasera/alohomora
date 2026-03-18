plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.room).apply(false)
    alias(libs.plugins.ksp).apply(false)
    alias(libs.plugins.buildConfig).apply(false)
}

allprojects {
    group = "io.github.yashkasera"
    version = "1.0.0"
}

apiValidation {
    ignoredProjects += listOf("showcaseApp", "desktopApp", "alohomora-common", "alohomora-ui")
}

tasks.register("publishGithubPackages") {
    group = "publishing"
    description = "Publishes alohomora and alohomora-noop artifacts to GitHub Packages"
    dependsOn(
        ":alohomora:publishAllPublicationsToGitHubPackagesRepository",
        ":alohomora-noop:publishAllPublicationsToGitHubPackagesRepository",
    )
}

tasks.register("verifyPublishingSetup") {
    group = "verification"
    description = "Verifies maven-publish wiring by publishing artifacts to Maven local"
    dependsOn(
        ":alohomora:publishToMavenLocal",
        ":alohomora-noop:publishToMavenLocal",
    )
}
