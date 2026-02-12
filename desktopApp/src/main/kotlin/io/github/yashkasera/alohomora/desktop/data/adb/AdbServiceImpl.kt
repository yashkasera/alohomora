package io.github.yashkasera.alohomora.desktop.data.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AdbServiceImpl(
    private val runner: AdbCommandRunner = DefaultAdbCommandRunner(),
) : AdbDataSource {
    override suspend fun listDevices(): List<AdbDevice> = withContext(Dispatchers.IO) {
        val result = runner.run(listOf("devices", "-l"))
        if (result.exitCode != 0) {
            throw IllegalStateException(result.stderr.ifBlank { "adb devices failed" })
        }
        AdbParser.parseDevices(result.stdout)
    }

    override suspend fun forwardDevToolsPort(deviceId: String, hostPort: Int, devicePort: Int) {
        runRequired(deviceId, listOf("forward", "tcp:$hostPort", "tcp:$devicePort"))
    }

    override suspend fun removeForward(deviceId: String, hostPort: Int) {
        runRequired(deviceId, listOf("forward", "--remove", "tcp:$hostPort"))
    }

    override suspend fun runCommand(deviceId: String?, args: List<String>): AdbCommandResult =
        withContext(Dispatchers.IO) {
            val command = if (deviceId.isNullOrBlank()) {
                args
            } else {
                listOf("-s", deviceId) + args
            }
            runner.run(command)
        }

    private suspend fun runRequired(deviceId: String, args: List<String>) {
        val result = runCommand(deviceId, args)
        if (result.exitCode != 0) {
            throw IllegalStateException(result.stderr.ifBlank { "adb ${args.joinToString(" ")} failed" })
        }
    }
}
