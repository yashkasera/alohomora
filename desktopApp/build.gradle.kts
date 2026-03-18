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
    implementation(libs.desktop.jvm.macos.arm64)

    implementation(libs.kotlinx.serialization.json)

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.logback.classic)
    // Ktor Client (WebSocket agent)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)

    implementation("org.jetbrains.pty4j:pty4j:0.13.11")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "io.github.yashkasera.alohomora.desktop.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Alohomora"
            packageVersion = "1.0.0"

            appResourcesRootDir.set(
                project.layout.projectDirectory.dir("src/main/resources")
            )

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
