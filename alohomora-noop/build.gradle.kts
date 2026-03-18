import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "io.github.yashkasera.alohomora.noop"
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Minimal dependencies - only what's needed for the API surface
            implementation(libs.compose.runtime)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            val githubOwner = providers.gradleProperty("github.owner").orNull ?: System.getenv("GITHUB_REPOSITORY_OWNER") ?: "yashkasera"
            val githubRepo = providers.gradleProperty("github.repo").orNull ?: rootProject.name
            url = uri("https://maven.pkg.github.com/$githubOwner/$githubRepo")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: System.getenv("GITHUB_REPOSITORY_OWNER")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GH_PACKAGES_TOKEN")
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(project.name)
            description.set("Alohomora no-op implementation for release builds")
            url.set("https://github.com/yashkasera/Alohomora")
        }
    }
}
