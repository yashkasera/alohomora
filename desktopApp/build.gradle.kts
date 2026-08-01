import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":alohomora-common"))
    implementation(project(":alohomora-ui"))

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.resources)
    implementation(libs.compose.material3)

    // currentOs, not a pinned macos-arm64 artifact: nativeDistributions targets Dmg, Msi
    // and Deb, and hardcoding the macOS Skia natives makes the Windows/Linux packages
    // structurally broken.
    implementation(compose.desktop.currentOs)

    implementation(libs.kotlinx.serialization.json)

    // Ktor client only. There is no embedded server here — the desktop app is a TCP
    // *client* of the in-app DevTools server over an adb port forward.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

    // Compose UI tests. Added because auto-scroll shipped completely inert — a snapshotFlow over
    // a plain Int that never re-emitted. It compiled and read correctly; only driving the real
    // composition catches that class of bug.
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "io.github.yashkasera.alohomora.desktop.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Alohomora"
            packageVersion = "1.0.0"

            // No appResourcesRootDir: pointing it at src/main/resources double-bundled every
            // classpath resource as an app resource. adb is located on the host at runtime
            // (see AdbLocator), never shipped — platform-tools redistribution carries licence
            // obligations and a pinned copy fights the user's own adb server.

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "io.github.yashkasera.alohomora.desktopApp"
            }
        }

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}
