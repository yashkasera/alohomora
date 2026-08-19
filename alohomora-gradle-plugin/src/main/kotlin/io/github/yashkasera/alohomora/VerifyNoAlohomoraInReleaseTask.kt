package io.github.yashkasera.alohomora

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Verification task; produces no output worth caching")
abstract class VerifyNoAlohomoraInReleaseTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aapt2Path: RegularFileProperty

    @TaskAction
    fun verify() {
        val aapt2 = aapt2Path.get().asFile
        val apk = apkDir.get().asFile.walkTopDown()
            .firstOrNull { it.extension == "apk" }
            ?: throw GradleException("[Alohomora] No APK found in ${apkDir.get().asFile}")

        val output = ProcessBuilder(
            aapt2.absolutePath, "dump", "xmltree", apk.absolutePath,
            "--file", "AndroidManifest.xml",
        )
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()

        if ("DevToolsActivity" in output) {
            throw GradleException(
                "[Alohomora] DevToolsActivity found in the release APK manifest.\n" +
                    "The alohomora library must only be included via debugImplementation.\n" +
                    "APK: ${apk.absolutePath}",
            )
        }
    }
}
