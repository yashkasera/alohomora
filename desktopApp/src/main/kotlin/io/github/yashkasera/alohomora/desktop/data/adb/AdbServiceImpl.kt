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

    override suspend fun enableTcpMode(deviceId: String, tcpPort: Int) {
        runRequired(deviceId, listOf("tcpip", tcpPort.toString()))
    }

    override suspend fun connect(host: String, port: Int): AdbCommandResult {
        return withContext(Dispatchers.IO) {
            runner.run(listOf("connect", "$host:$port"))
        }
    }

    override suspend fun disconnect(host: String, port: Int): AdbCommandResult {
        return withContext(Dispatchers.IO) {
            runner.run(listOf("disconnect", "$host:$port"))
        }
    }

    override suspend fun pair(host: String, port: Int, code: String): AdbCommandResult {
        return withContext(Dispatchers.IO) {
            // Code as its own arg: omitting it makes `adb pair` block waiting for interactive input.
            runner.run(listOf("pair", "$host:$port", code))
        }
    }

    override suspend fun listMdnsServices(): AdbCommandResult {
        return withContext(Dispatchers.IO) {
            runner.run(listOf("mdns", "services"))
        }
    }

    override suspend fun restartServer(): AdbCommandResult {
        return withContext(Dispatchers.IO) {
            val kill = runner.run(listOf("kill-server"))
            if (kill.exitCode != 0) {
                return@withContext kill
            }
            runner.run(listOf("start-server"))
        }
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

    override suspend fun runDetached(deviceId: String?, args: List<String>): String? =
        withContext(Dispatchers.IO) {
            val command = if (deviceId.isNullOrBlank()) args else listOf("-s", deviceId) + args
            runner.runDetached(command)
        }

    private suspend fun runRequired(deviceId: String, args: List<String>) {
        val result = runCommand(deviceId, args)
        if (result.exitCode != 0) {
            throw IllegalStateException(result.stderr.ifBlank { "adb ${args.joinToString(" ")} failed" })
        }
    }
}
