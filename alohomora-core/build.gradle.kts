import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    id("alohomora.publish")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "io.github.yashkasera.alohomora.core"
        // Same floor as :alohomora — see the note there before raising either.
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        // Same reason as :alohomora: without it commonTest never reaches the Android host.
        withHostTest {}
    }

    // Kept in lockstep with :alohomora.
    val xcf = XCFramework("AlohomoraKitCore")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("io.github.yashkasera.alohomora.InternalAlohomoraApi")
        }

        commonMain.dependencies {
            api(project(":alohomora-common"))

            implementation(libs.kermit)
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }

        androidMain.dependencies {
            // api: the OkHttp interceptors implement okhttp3.Interceptor publicly.
            api(libs.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }

    // Standalone capture-only framework (no Compose/Skia). Not distributed yet; the full
    // AlohomoraKit exports this module rather than embedding a second copy of it.
    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "AlohomoraKitCore"
                    isStatic = false
                    xcf.add(this)
                }
            }
        }
}
