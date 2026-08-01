import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    // Published because :alohomora's POM lists it as a runtime dependency.
    id("alohomora.publish")
}

kotlin {
    jvmToolchain(17)
    android {
        namespace = "io.github.yashkasera.alohomora.ui"
        compileSdk = 37
        minSdk = 24
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.resources)
            api(libs.compose.ui.tooling.preview)
            api(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            // No platform-specific UI deps yet.
        }

        jvmMain.dependencies {
            // Desktop app provides compose.desktop.currentOs.
        }

        iosMain.dependencies {
            // No platform-specific deps required.
        }
    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "AlohomoraUi"
                    isStatic = true
                }
            }
        }
}
