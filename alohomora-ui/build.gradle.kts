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
        // Kept at the same floor as :alohomora — see the note there before raising it.
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // The waterfall renderer draws Span/TraceRow directly, and the tree assembly, time scaling
            // and trace summarising behind it are pure shared logic that must not be written twice —
            // once for the desktop panel and once for the mobile screen. Safe in both directions:
            // :alohomora-common has no Compose dependency, so there is no cycle, and the target sets
            // match exactly. Consumers already resolve it transitively via :alohomora, so the
            // published POM gains a name rather than a jar.
            api(project(":alohomora-common"))
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.resources)
            api(libs.compose.ui.tooling.preview)
            api(libs.kotlinx.serialization.json)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
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
