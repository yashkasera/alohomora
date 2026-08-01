import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    // Published because :alohomora's POM lists it as a runtime dependency.
    id("alohomora.publish")
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "io.github.yashkasera.alohomora.common"
        compileSdk = 37
        minSdk = 24
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm()

    // Kept in lockstep with :alohomora — a consumer must be able to resolve the same
    // targets for both the debug and release artifacts.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.network)
            implementation(libs.kotlinx.datetime)
        }
    }
}
