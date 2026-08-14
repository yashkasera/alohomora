package io.github.yashkasera.alohomora.desktop.data.ios

import io.github.yashkasera.alohomora.desktop.data.process.ProcessRunner
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A booted iOS simulator. */
data class SimulatorDevice(
    val udid: String,
    val name: String,
    /**
     * Simulator data container on the host filesystem.
     *
     * Useful beyond discovery: the app's sandbox (and therefore its SQLite files) lives under
     * here, which is how the Vault can inspect a simulator without any device-side support.
     */
    val dataPath: String?,
)

/**
 * Reads booted simulators via `xcrun simctl`.
 *
 * Simulators need **no tunnel**. A simulator is a macOS process using the host's network
 * stack, so a server bound to `127.0.0.1:53999` inside it is reachable at `127.0.0.1:53999` on
 * the Mac directly. Verified: a listener started inside a booted simulator showed up in the
 * host's own `lsof` output and accepted a plain host-side connection.
 */
class SimctlClient(
    private val xcrunPath: String = DEFAULT_XCRUN,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun isAvailable(): Boolean = File(xcrunPath).canExecute()

    /**
     * Booted simulators only. A shut-down simulator runs no app and so has nothing to inspect.
     */
    fun listBootedSimulators(): List<SimulatorDevice> {
        if (!isAvailable()) return emptyList()
        val result =
            ProcessRunner.run(listOf(xcrunPath, "simctl", "list", "-j", "devices", "booted"))
        if (!result.isSuccess) return emptyList()

        return try {
            json.parseToJsonElement(result.stdout)
                .jsonObject["devices"]
                ?.jsonObject
                // Keys are runtime identifiers (com.apple.CoreSimulator.SimRuntime.iOS-26-0);
                // every value is that runtime's device array.
                ?.values
                ?.flatMap { runtime -> runtime.jsonArray }
                ?.mapNotNull { element ->
                    val obj = element.jsonObject
                    val udid = obj["udid"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    SimulatorDevice(
                        udid = udid,
                        name = obj["name"]?.jsonPrimitive?.content ?: udid,
                        dataPath = obj["dataPath"]?.jsonPrimitive?.content,
                    )
                }
                .orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        /** Always present when the Xcode command line tools are installed. */
        const val DEFAULT_XCRUN = "/usr/bin/xcrun"
    }
}
