plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
    id("alohomora.publish")
}

group = "io.github.yashkasera"

// A literal fallback, not `rootProject.version`: this is a standalone build with its own settings
// file, so `rootProject` is this project and the version would resolve to "unspecified". CI passes
// -Palohomora.version on tag pushes; keep the default in step with the root build.
version = providers.gradleProperty("alohomora.version").getOrElse("1.0.0")

repositories {
    google()
    mavenCentral()
}

gradlePlugin {
    website = "https://yashkasera.github.io/alohomora"
    vcsUrl = "https://github.com/yashkasera/Alohomora"

    plugins {
        create("alohomora") {
            id = "io.github.yashkasera.alohomora"
            implementationClass =
                "io.github.yashkasera.alohomora.AlohomoraPlugin"
            displayName = "Alohomora"
            description = "Injects Git and build metadata into Alohomora debug builds"
            tags = listOf("android", "debugging", "observability", "devtools")
        }
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:9.2.1")
}
