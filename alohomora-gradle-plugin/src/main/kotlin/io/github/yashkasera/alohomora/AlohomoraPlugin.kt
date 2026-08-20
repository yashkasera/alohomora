package io.github.yashkasera.alohomora

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.LibraryVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
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
                configureReleaseVerification(project, androidComponents, variant)
                return@onVariants
            }
            println("Generating Alohomora for ${variant.name}")

            val task = project.tasks.register(
                "generateAlohomora${variant.name.replaceFirstChar { it.uppercase() }}Config",
                GenerateAlohomoraConfigTask::class.java,
            ) {
                appName.set(extension.appName)
                projectName.set(project.name)
                val resolvedPackageName = when (variant) {
                    is ApplicationVariant -> variant.applicationId.orNull
                    is LibraryVariant -> variant.namespace.orNull
                    else -> null
                }
                if (!resolvedPackageName.isNullOrBlank()) {
                    packageName.set(resolvedPackageName)
                }
                maxCommits.set(extension.maxCommits)
                slackWebhookUrl.set(extension.slackWebhookUrl)
                // ApplicationVariant exposes debuggable; a library variant has no such flag,
                // so treat it as non-debuggable and withhold the webhook.
                debuggable.set((variant as? ApplicationVariant)?.debuggable ?: false)
                variantName.set(variant.name)
                flavorName.set(variant.flavorName)
                buildType.set(variant.buildType)
                versionName.set(extension.versionName ?: project.version.toString())
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

            val iconTask = project.tasks.register(
                "generateAlohomora${variant.name.replaceFirstChar { it.uppercase() }}DevIcon",
                GenerateDevIconTask::class.java,
            ) {
                baseIconName.set(extension.devIconBase)
                bgRef.set(extension.devIconBgRef)
                val pos = extension.devIconVariantBarPosition
                if (pos != DevIconVariantBarPosition.NONE) {
                    variantBarText.set(variant.name.uppercase())
                    variantBarPosition.set(pos.name)
                }
                val appName = extension.appName
                    ?: readAppLabel(project)
                    ?: project.name
                label.set(extension.devIconLabel ?: "$appName · ${variant.name}")
                outputDir.set(
                    project.layout.buildDirectory.dir(
                        "generated/res/devicon/${variant.name}",
                    ),
                )
            }

            variant.sources.res?.addGeneratedSourceDirectory(
                iconTask,
                GenerateDevIconTask::outputDir,
            )
        }
    }

    private fun readAppLabel(project: Project): String? {
        val manifest = project.file("src/main/AndroidManifest.xml")
        if (!manifest.exists()) return null
        val match = Regex("""android:label="([^"@]+)"""").find(manifest.readText())
        return match?.groupValues?.get(1)
    }

    private fun configureReleaseVerification(
        project: Project,
        androidComponents: AndroidComponentsExtension<*, *, *>,
        variant: com.android.build.api.variant.Variant,
    ) {
        if (variant !is ApplicationVariant) return

        val aapt2 = androidComponents.sdkComponents.aapt2.map { it.executable.get() }

        val verifyTask = project.tasks.register(
            "verifyNoAlohomoraIn${variant.name.replaceFirstChar { it.uppercase() }}",
            VerifyNoAlohomoraInReleaseTask::class.java,
        ) {
            apkDir.set(variant.artifacts.get(SingleArtifact.APK))
            aapt2Path.set(aapt2)
        }

        val assembleTaskName = "assemble${variant.name.replaceFirstChar { it.uppercase() }}"
        project.tasks.matching { it.name == assembleTaskName }.configureEach {
            finalizedBy(verifyTask)
        }
    }
}
