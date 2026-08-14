package io.github.yashkasera.alohomora.desktop.data.adb

import io.github.yashkasera.alohomora.desktop.data.process.ProcessRunner
import java.util.concurrent.TimeUnit

/**
 * Runs `adb`, resolving the executable via [AdbLocator] and delegating process handling to
 * [ProcessRunner].
 *
 * Both halves matter. Resolution fixes packaged builds, where a GUI process has no shell PATH
 * and every ADB feature silently failed. [ProcessRunner] fixes the stderr pipe deadlock, the
 * missing timeout, and the orphaned-process-on-cancel leak.
 */
internal class DefaultAdbCommandRunner(
    private val timeout: Long = ProcessRunner.DEFAULT_TIMEOUT_SECONDS,
    private val timeoutUnit: TimeUnit = TimeUnit.SECONDS,
) : AdbCommandRunner {

    override fun run(args: List<String>): AdbCommandResult {
        val adb = AdbLocator.find() ?: return adbMissing()
        val result = ProcessRunner.run(listOf(adb) + args, timeout, timeoutUnit)
        return AdbCommandResult(result.exitCode, result.stdout, result.stderr)
    }

    override fun runDetached(args: List<String>): String? {
        val adb = AdbLocator.find() ?: return ADB_MISSING_MESSAGE
        return ProcessRunner.runDetached(listOf(adb) + args)
    }

    private fun adbMissing() =
        AdbCommandResult(exitCode = -1, stdout = "", stderr = ADB_MISSING_MESSAGE)

    private companion object {
        const val ADB_MISSING_MESSAGE =
            "adb not found. Set ANDROID_HOME, or pass -Dalohomora.adb.path=/path/to/adb."
    }
}
