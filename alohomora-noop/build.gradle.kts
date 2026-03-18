import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
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
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
