package io.github.yashkasera.alohomora

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateAlohomoraConfigTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val resourcesDir: DirectoryProperty

    @get:Input
    abstract val maxCommits: Property<Int>

    @get:Input
    @get:Optional
    abstract val slackWebhookUrl: Property<String>

    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    @get:Optional
    abstract val packageName: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    @get:Optional
    abstract val flavorName: Property<String>

    @get:Input
    @get:Optional
    abstract val buildType: Property<String>

    /**
     * Whether the target variant is debuggable.
     *
     * Gates [slackWebhookUrl]: the URL is emitted as a plaintext `String` constant, so in a
     * non-debuggable variant it ships in production dex and is recoverable with `strings`.
     * Nothing but `enabledVariants` previously stood between a live webhook and a release APK.
     */
    @get:Input
    abstract val debuggable: Property<Boolean>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun generate() {
        val outputFile = outputDir.get()
            .file("AlohomoraBuildGenerationConfig.kt")
            .asFile

        val commits = try {
            ProcessBuilder(
                "git",
                "log",
                "-${maxCommits.get()}",
                // Use the ASCII unit separator (0x1F) as the field delimiter:
                // commit subjects (%s) routinely contain '|', which would corrupt
                // a '|'-delimited split and shift fields into the wrong slots.
                "--pretty=format:%h%x1f%an%x1f%s%x1f%ct",
            )
                .start()
                .inputStream
                .bufferedReader()
                .readLines()
        } catch (_: Exception) {
            emptyList()
        }

        outputFile.writeText(generateSource(commits))

        // Write the service file to resources
        val serviceFile = resourcesDir.get()
            .dir("META-INF/services")
            .file("io.github.yashkasera.alohomora.data.model.AlohomoraConfig")
            .asFile

        serviceFile.parentFile.mkdirs()
        serviceFile.writeText("alohomora.generated.AlohomoraBuildGenerationInfo")
    }

    private fun generateSource(commits: List<String>): String {
        val branch = git(listOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
        val sha = git(listOf("git", "rev-parse", "--short", "HEAD"))
        val dirty = git(listOf("git", "status", "--porcelain")).isNotEmpty()
        // Milliseconds, like every other timestamp in the project. This used to be seconds,
        // which the desktop dashboard rendered with the format's implicit seconds default while
        // the Chronicle panel had to override it — the sort of split that produced 1970 dates.
        val timestamp = System.currentTimeMillis()
        val isDebuggable = debuggable.getOrElse(false)
        val slackUrl = slackWebhookUrl.orNull?.takeIf { url ->
            if (!isDebuggable && url.isNotBlank()) {
                logger.warn(
                    "[Alohomora] slackWebhookUrl is set but variant '${variantName.get()}' is " +
                        "not debuggable; omitting it so the secret does not ship in release dex.",
                )
                false
            } else {
                true
            }
        }
        val projectName = projectName.get()
        val packageName = packageName.orNull
        val versionName = versionName.get()
        val versionCode = versionCode.get()
        val variantName = variantName.get()
        val flavorName = flavorName.orNull
        val buildType = buildType.orNull

        return buildString {
            appendLine("package alohomora.generated")
            appendLine()
            appendLine("import io.github.yashkasera.alohomora.data.model.AlohomoraConfig")
            appendLine("import io.github.yashkasera.alohomora.data.model.AlohomoraCommit")
            appendLine()
            appendLine("class AlohomoraBuildGenerationInfo : AlohomoraConfig {")
            appendLine()
            appendLine("    companion object {")
            appendLine("        val INSTANCE = AlohomoraBuildGenerationInfo()")
            appendLine("    }")
            appendLine()
            appendLine()
            appendLine("    override val slackWebhookUrl: String? = ${escape(slackUrl)}")
            appendLine()
            appendLine("    override val projectName: String = ${escape(projectName)}")
            appendLine()
            appendLine("    override val packageName: String? = ${escape(packageName)}")
            appendLine()
            appendLine("    override val versionName: String = ${escape(versionName)}")
            appendLine()
            appendLine("    override val versionCode: Int = $versionCode")
            appendLine()
            appendLine("    override val variantName: String = ${escape(variantName)}")
            appendLine()
            appendLine("    override val flavorName: String? = ${escape(flavorName)}")
            appendLine()
            appendLine("    override val buildType: String? = ${escape(buildType)}")
            appendLine()
            appendLine("    override val branch: String = ${escape(branch)}")
            appendLine()
            appendLine("    override val commitSha: String = ${escape(sha)}")
            appendLine()
            appendLine("    override val isDirty: Boolean = $dirty")
            appendLine()
            appendLine("    override val buildTimestampUtc: Long = ${timestamp}L")
            appendLine()
            appendLine("    override val commits: List<AlohomoraCommit> = listOf(")

            commits.forEach { commit ->
                val parts = commit.split("", limit = 4)
                if (parts.size != 4) return@forEach

                val sha = parts[0]
                val author = escape(parts[1], false)
                val message = escape(parts[2], false)
                // git %ct is epoch SECONDS; convert once here so nothing downstream has to
                // remember that commits are the odd one out. Both Chronicle screens formatted
                // this as milliseconds and rendered every commit as 1970-01-21.
                val timestamp = parts[3].toLongOrNull()?.times(1_000) ?: 0L

                appendLine(
                    """
                AlohomoraCommit(
                    sha = "$sha",
                    author = "$author",
                    message = "$message",
                    timestamp = ${timestamp}L
                ),
                """.trimIndent().prependIndent("        "),
                )
            }

            appendLine("    )")
            appendLine("}")
        }
    }

    private fun git(command: List<String>): String =
        ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()

    private fun escape(value: String?, wrap: Boolean = true): String? =
        value?.replace("\\", "\\\\")
            ?.replace("\"", "\\\"")
            ?.replace("$", "\\$")
            ?.replace("\n", "\\n")
            ?.let { if (wrap) "\"$it\"" else it }

}
