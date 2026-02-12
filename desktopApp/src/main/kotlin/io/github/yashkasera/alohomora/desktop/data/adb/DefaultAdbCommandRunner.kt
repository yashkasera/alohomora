package io.github.yashkasera.alohomora.desktop.data.adb

import java.io.BufferedReader
import java.io.InputStreamReader

internal class DefaultAdbCommandRunner : AdbCommandRunner {
    override fun run(args: List<String>): AdbCommandResult {
        val process = ProcessBuilder(listOf("adb") + args)
            .redirectErrorStream(false)
            .start()
        val stdout = readStream(process.inputStream)
        val stderr = readStream(process.errorStream)
        val exitCode = process.waitFor()
        return AdbCommandResult(exitCode, stdout.trimEnd(), stderr.trimEnd())
    }

    private fun readStream(stream: java.io.InputStream): String {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            return reader.readText()
        }
    }
}
