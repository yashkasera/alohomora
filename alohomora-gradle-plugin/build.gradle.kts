plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
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
    plugins {
        create("alohomora") {
            id = "io.github.yashkasera.alohomora"
            implementationClass =
                "io.github.yashkasera.alohomora.AlohomoraPlugin"
        }
    }
}

dependencies {
    compileOnly("com.android.tools.build:gradle:9.2.1")
}
