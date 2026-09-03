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

val desktopVersion: String = providers.gradleProperty("alohomora.desktop.version")
    .getOrElse("0.0.0")

val generateVersionProperties by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources/version")
    val version = desktopVersion
    outputs.dir(outputDir)
    doLast {
        val propsFile = outputDir.get().file("alohomora-desktop-version.properties").asFile
        propsFile.parentFile.mkdirs()
        propsFile.writeText("version=$version\n")
    }
}

sourceSets.main {
    resources.srcDir(generateVersionProperties.map { it.outputs.files.singleFile })
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

    // QR encoding for Wireless-debugging "Pair with QR code".
    implementation(libs.zxing.core)

    // Ktor client — the desktop app is a TCP *client* of the in-app DevTools server over an
    // adb port forward.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)

    // Ktor server — the read-only MCP server's only inbound listener, bound to loopback. The MCP
    // SDK ships no engine of its own, so we supply CIO here at the same Ktor version as the client.
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.sse)

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

        jvmArgs += listOf(
            "-Xdock:name=Alohomora",
            "-Xdock:icon=${project.file("appIcons/MacosIcon.icns").absolutePath}",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Alohomora"
            packageVersion = desktopVersion

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "io.github.yashkasera.alohomora.AlohomoraApp"
            }
        }
        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}
