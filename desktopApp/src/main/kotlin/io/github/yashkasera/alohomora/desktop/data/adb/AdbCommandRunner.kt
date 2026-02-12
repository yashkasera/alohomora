package io.github.yashkasera.alohomora.desktop.data.adb

internal interface AdbCommandRunner {
    fun run(args: List<String>): AdbCommandResult
}
