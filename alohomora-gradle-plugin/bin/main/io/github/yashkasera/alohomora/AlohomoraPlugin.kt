package io.github.yashkasera.alohomora

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AlohomoraPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("🔥 Alohomora applied to ${project.path}")

        val extension = project.extensions.create(
            "alohomora",
            GitHistoryExtension::class.java,
        )
        project.pluginManager.withPlugin("com.android.application") {
            configureAndroid(project, extension)
        }

        project.pluginManager.withPlugin("com.android.library") {
            configureAndroid(project, extension)
        }

    }

    private fun configureAndroid(
        project: Project,
        extension: GitHistoryExtension,
    ) {
        val androidComponents =
            project.extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            if (!extension.enabledVariants.contains(variant.name)) {
                println("Skipping Alohomora for ${variant.name}")
                return@onVariants
            }

            println("Generating Alohomora for ${variant.name}")

            val task = project.tasks.register(
                "generateAlohomoraGitHistory${variant.name.replaceFirstChar { it.uppercase() }}",
                GenerateGitHistoryTask::class.java,
            ) {
                maxCommits.set(extension.maxCommits)
                outputDir.set(
                    project.layout.buildDirectory.dir(
                        "generated/alohomora/${variant.name}")
                )
            }

            variant.sources.java?.addGeneratedSourceDirectory(
                task,
                GenerateGitHistoryTask::outputDir
            )

            variant.sources.kotlin?.addGeneratedSourceDirectory(
                task,
                GenerateGitHistoryTask::outputDir
            )
        }
    }

}
