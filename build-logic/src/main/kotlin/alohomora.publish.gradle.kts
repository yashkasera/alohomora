import com.vanniktech.maven.publish.SonatypeHost

plugins {
    com.vanniktech.maven.publish
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set(project.name)
        description.set(
            providers.gradleProperty("alohomora.pom.description").orNull
                ?: "Alohomora developer observability toolkit",
        )
        url.set("https://github.com/yashkasera/Alohomora")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("yashkasera")
                name.set("Yash Kasera")
                url.set("https://github.com/yashkasera")
            }
        }
        scm {
            url.set("https://github.com/yashkasera/Alohomora")
            connection.set("scm:git:https://github.com/yashkasera/Alohomora.git")
            developerConnection.set("scm:git:ssh://git@github.com/yashkasera/Alohomora.git")
        }
    }
}

// Only Android and KotlinMultiplatform (metadata) publications belong on Central. iOS consumers
// use SPM via the XCFramework, JVM is only used internally by desktopApp as a project dependency,
// and no external consumer resolves either. Disable their publish/sign tasks.
afterEvaluate {
    val skipPattern = Regex("(?i)(ios|watchos|tvos|macos|jvm)")
    tasks.configureEach {
        if (skipPattern.containsMatchIn(name) && (name.startsWith("publish") || name.startsWith("sign"))) {
            enabled = false
        }
    }
}
