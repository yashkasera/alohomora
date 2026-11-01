import com.vanniktech.maven.publish.SonatypeHost

plugins {
    com.vanniktech.maven.publish
}

mavenPublishing {
    // automaticRelease: without it the Central Portal holds every upload as a validated but
    // unreleased deployment until someone clicks Publish. release.yml never did, so 1.1.0 and 1.3.0
    // went green in CI and never reached Maven Central, while the Gradle Plugin Portal (which has
    // no such step) did get them. Plugin 1.3.0 then generated code against a library that only
    // existed as 1.2.0 on Central, and consumers pairing the two failed to compile.
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    pom {
        name.set(project.name)
        description.set(
            providers.gradleProperty("alohomora.pom.description").orNull
                ?: "Alohomora developer observability toolkit",
        )
        url.set("https://yashkasera.github.io/alohomora")

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
