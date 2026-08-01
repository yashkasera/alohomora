import org.gradle.api.publish.maven.MavenPublication

/**
 * Publishing convention for every Alohomora artifact.
 *
 * Extracted because the GitHub Packages repository block was copy-pasted verbatim across
 * modules and was about to be duplicated a fourth time when `:alohomora-common` and
 * `:alohomora-ui` became publishable. Those two MUST be published: they appear as runtime
 * dependencies in `:alohomora`'s POM, so leaving them out gives every external consumer two
 * unresolvable dependencies.
 *
 * This is also the single place where POM completeness lives — licence, SCM and developer
 * blocks are mandatory for Maven Central and were previously absent everywhere.
 */
plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            val githubOwner = providers.gradleProperty("github.owner").orNull
                ?: System.getenv("GITHUB_REPOSITORY_OWNER")
                ?: "yashkasera"
            val githubRepo = providers.gradleProperty("github.repo").orNull ?: "Alohomora"
            url = uri("https://maven.pkg.github.com/$githubOwner/$githubRepo")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: System.getenv("GITHUB_REPOSITORY_OWNER")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GH_PACKAGES_TOKEN")
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
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
}
