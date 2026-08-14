package io.github.yashkasera.alohomora.desktop.data.process

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/** Outcome of a completed external process. */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Runs external processes safely.
 *
 * Extracted so every tool the desktop app shells out to — `adb`, `xcrun simctl`,
 * `devicectl` — shares one correct implementation. The original ad-hoc version had three
 * defects worth not reintroducing per call site:
 *
 *  1. **Pipe deadlock.** Draining stdout to EOF before touching stderr hangs forever once the
 *     child fills the ~64 KB stderr buffer (`adb install`, `adb bugreport`, `simctl` on error
 *     all do this). Both streams are therefore drained concurrently.
 *  2. **No timeout.** An unbounded `waitFor()` parks the calling thread permanently.
 *  3. **No cleanup.** Cancellation orphaned the child process.
 */
object ProcessRunner {

    const val DEFAULT_TIMEOUT_SECONDS = 30L

    /**
     * Runs [command] to completion.
     *
     * Never throws for process-level failures; a launch failure is reported as exit code -1
     * with the reason on stderr, so callers can treat "tool missing" and "tool failed" alike.
     */
    fun run(
        command: List<String>,
        timeout: Long = DEFAULT_TIMEOUT_SECONDS,
        timeoutUnit: TimeUnit = TimeUnit.SECONDS,
    ): ProcessResult {
        if (command.isEmpty()) return ProcessResult(-1, "", "empty command")

        val process = try {
            ProcessBuilder(command).start()
        } catch (e: Exception) {
            return ProcessResult(-1, "", "Failed to launch ${command.first()}: ${e.message}")
        }

        val stdout = StreamDrainer(process.inputStream, "${command.first()}-stdout")
        val stderr = StreamDrainer(process.errorStream, "${command.first()}-stderr")

        return try {
            if (!process.waitFor(timeout, timeoutUnit)) {
                process.destroyForcibly()
                ProcessResult(
                    exitCode = -1,
                    stdout = stdout.text(),
                    stderr = "${command.joinToString(" ")} timed out after $timeout $timeoutUnit",
                )
            } else {
                ProcessResult(process.exitValue(), stdout.text().trimEnd(), stderr.text().trimEnd())
            }
        } catch (e: InterruptedException) {
            // Coroutine cancellation arrives as a thread interrupt. Kill the child instead of
            // orphaning it, and re-assert the interrupt for the caller.
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            ProcessResult(-1, "", "${command.first()} interrupted")
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    /**
     * Starts [command] without awaiting it, for processes that run until signalled
     * (`adb shell screenrecord`). Output is discarded so an unread pipe cannot fill and stall
     * the child mid-run.
     *
     * @return null on success, or an error message.
     */
    fun runDetached(command: List<String>): String? {
        if (command.isEmpty()) return "empty command"
        return try {
            val process = ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            if (!process.isAlive && process.exitValue() != 0) {
                "${command.joinToString(" ")} exited immediately with ${process.exitValue()}"
            } else {
                null
            }
        } catch (e: Exception) {
            "Failed to launch ${command.first()}: ${e.message}"
        }
    }

    /** Drains one stream on a daemon thread so neither pipe can block the other. */
    private class StreamDrainer(stream: InputStream, name: String) {
        private val sb = StringBuilder()
        private val thread = Thread(
            {
                runCatching {
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = reader.read(buffer)
                            if (read < 0) break
                            synchronized(sb) { sb.appendRange(buffer, 0, read) }
                        }
                    }
                }
            },
            name,
        ).apply {
            isDaemon = true
            start()
        }

        fun text(): String {
            thread.join(JOIN_TIMEOUT_MILLIS)
            return synchronized(sb) { sb.toString() }
        }

        private companion object {
            const val JOIN_TIMEOUT_MILLIS = 2_000L
        }
    }
}
