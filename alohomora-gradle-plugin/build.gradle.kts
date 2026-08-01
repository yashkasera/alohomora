plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "io.github.yashkasera"
version = rootProject.version

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
            version = rootProject.version
        }
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:9.2.1")
}
