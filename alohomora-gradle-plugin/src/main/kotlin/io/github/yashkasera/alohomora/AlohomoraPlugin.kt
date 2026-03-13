package io.github.yashkasera.alohomora

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AlohomoraPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("🔥 Alohomora applied to ${project.path}")

        val extension = project.extensions.create(
            "alohomora",
            AlohomoraExtension::class.java,
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
        extension: AlohomoraExtension,
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
                "generateAlohomora${variant.name.replaceFirstChar { it.uppercase() }}Config",
                GenerateAlohomoraConfigTask::class.java,
            ) {
                projectName.set(project.name)
                maxCommits.set(extension.maxCommits)
                slackWebhookUrl.set(extension.slackWebhookUrl)
                variantName.set(variant.name)
                flavorName.set(variant.flavorName)
                buildType.set(variant.buildType)
                versionName.set(extension.versionName)
                versionCode.set(extension.versionCode)
                val outputDirectory = project.layout.buildDirectory.dir(
                    "generated/alohomora/${variant.name}",
                )
                outputDir.set(outputDirectory)
                resourcesDir.set(outputDirectory.map { it.dir("resources") })
            }


            variant.sources.java?.addGeneratedSourceDirectory(
                task,
                GenerateAlohomoraConfigTask::outputDir,
            )

            variant.sources.kotlin?.addGeneratedSourceDirectory(
                task,
                GenerateAlohomoraConfigTask::outputDir,
            )

            variant.sources.resources?.addGeneratedSourceDirectory(
                task,
                GenerateAlohomoraConfigTask::resourcesDir,
            )

        }
    }

}
