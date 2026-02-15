package io.github.yashkasera.alohomora

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*

abstract class GenerateGitHistoryTask : DefaultTask() {

    @get:Input
    abstract val maxCommits: Property<Int>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile = outputDir.get()
            .file("AlohomoraBuildGenerationInfo.kt")
            .asFile

        val commits = try {
            ProcessBuilder(
                "git",
                "log",
                "-${maxCommits.get()}",
                "--pretty=format:%h|%an|%s|%ct"
            )
                .start()
                .inputStream
                .bufferedReader()
                .readLines()
        } catch (_: Exception) {
            emptyList()
        }

        outputFile.writeText(generateSource(commits))
    }

    private fun generateSource(commits: List<String>): String {
        val branch = git(listOf("git", "rev-parse", "--abbrev-ref", "HEAD"))
        val sha = git(listOf("git", "rev-parse", "--short", "HEAD"))
        val dirty = git(listOf("git", "status", "--porcelain")).isNotEmpty()
        val timestamp = System.currentTimeMillis() / 1000


        return buildString {
            appendLine("package io.github.yashkasera.alohomora.generated")
            appendLine()
            appendLine("import io.github.yashkasera.alohomora.data.model.Commit")
            appendLine()
            appendLine("object AlohomoraBuildGenerationInfo {")
            appendLine()
            appendLine()
            appendLine("    val branch: String = \"${escape(branch)}\"")
            appendLine()
            appendLine("    val commitSha: String = \"${escape(sha)}\"")
            appendLine()
            appendLine("    val isDirty: Boolean = $dirty")
            appendLine()
            appendLine("    val buildTimestampUtc: Long = ${timestamp}L")
            appendLine()
            appendLine("    val commits: List<Commit> = listOf(")

            commits.forEach { commit ->
                val parts = commit.split("|", limit = 4)
                if (parts.size != 4) return@forEach

                val sha = parts[0]
                val author = escape(parts[1])
                val message = escape(parts[2])
                val timestamp = parts[3]

                appendLine(
                    """
                Commit(
                    sha = "$sha",
                    author = "$author",
                    message = "$message",
                    timestamp = ${timestamp}L
                ),
                """.trimIndent().prependIndent("        ")
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


    private fun escape(value: String): String =
        value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

}
