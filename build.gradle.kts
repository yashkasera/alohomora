plugins {
    alias(libs.plugins.binary.compatibility.validator)
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
}

allprojects {
    group = "io.github.yashkasera"
    version = "1.0.0"
}

apiValidation {
    ignoredProjects += listOf("showcaseApp", "desktopApp", "alohomora-common", "alohomora-ui")

    // Without this, :alohomora:apiCheck is a no-op: BCV only dumps JVM targets by default,
    // :alohomora has no jvm() target, and klib validation is disabled out of the box. The
    // repo previously claimed apiCheck guarded the public surface while enforcing nothing.
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

// All four modules must ship together: :alohomora's POM lists :alohomora-common and
// :alohomora-ui as runtime dependencies, so publishing only the first two leaves every
// external consumer with unresolvable dependencies.
val publishedProjects = listOf(":alohomora", ":alohomora-noop", ":alohomora-common", ":alohomora-ui")

tasks.register("publishGithubPackages") {
    group = "publishing"
    description = "Publishes all Alohomora artifacts to GitHub Packages"
    dependsOn(publishedProjects.map { "$it:publishAllPublicationsToGitHubPackagesRepository" })
}

tasks.register("verifyPublishingSetup") {
    group = "verification"
    description = "Verifies maven-publish wiring by publishing artifacts to Maven local"
    dependsOn(publishedProjects.map { "$it:publishToMavenLocal" })
}
