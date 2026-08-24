import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    id("alohomora.publish")
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "io.github.yashkasera.alohomora.noop"
        // Kept at the same floor as :alohomora — see the note there before raising it.
        compileSdk = 36
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
            // Minimal dependencies - only what's needed for the API surface.
            // Deliberately NOT :alohomora-ui — pulling the design system in here would
            // defeat the point of the no-op module. compose.ui is needed only for the
            // ImageVector type in CustomScreenPlugin, and every Compose app already has it.
            api(libs.compose.ui)
            implementation(libs.compose.runtime)
            implementation(libs.ktor.client.core)
        }

        androidMain.dependencies {
            // Mirrors :alohomora — TraceInterceptor implements okhttp3.Interceptor.
            api(libs.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            compileOnly(libs.firebase.config)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
