plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.yashkasera"

// A literal, not `rootProject.version`: this is now a standalone build with its own settings file,
// so `rootProject` is this project and the version would resolve to "unspecified". Keep in step with
// the `allprojects` version in the root build.
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("alohomora") {
            id = "io.github.yashkasera.alohomora"
            implementationClass =
                "io.github.yashkasera.alohomora.AlohomoraPlugin"
        }
    }
}

dependencies {
    // compileOnly, never implementation: a consumer applying this plugin already has its own AGP on
    // the buildscript classpath, and shipping ours as a runtime dependency drags AGP 9.2.1 in beside
    // it — Gradle resolves the higher version and silently upgrades the consumer's AGP. Every API
    // this plugin touches (AndroidComponentsExtension, onVariants, sources.*.addGeneratedSourceDirectory)
    // has been stable since AGP 7.x, so the consumer's own AGP supplies them at execution time and
    // the plugin works on AGP 8 as well as 9.
    compileOnly("com.android.tools.build:gradle:9.2.1")
}
