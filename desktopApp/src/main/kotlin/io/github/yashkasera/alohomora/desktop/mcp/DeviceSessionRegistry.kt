package io.github.yashkasera.alohomora.desktop.mcp

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One connected device window, as the MCP tools see it.
 *
 * [model] and [platform] are the ADB-reported values, carried here so `list_devices` can name a
 * device without the tool layer reaching back into the devices view model. They are nullable because
 * a session can open before ADB has reported them.
 *
 * [networkRulesViewModel] is carried so the write tools (mocks, throttle) go through the *same* source
 * of truth the desktop UI edits — otherwise the agent's change would not show in the console and would
 * be clobbered by the next UI edit. Reads and replay/clear use [devToolsRepository] directly.
 */
data class DeviceSessionHandle(
    val deviceId: String,
    val model: String?,
    val platform: String?,
    val devToolsRepository: DevToolsRepository,
    val networkRulesViewModel: NetworkRulesViewModel,
)

/**
 * The bridge between an app-scoped MCP server (exactly one listener) and per-device-window data
 * (one [DevToolsRepository] per open window).
 *
 * `Main.kt` owns this and keeps it in step with its `sessions` list; the tool layer reads
 * [sessions] to resolve a `deviceId` argument to the right window's captured data. A plain holder on
 * purpose — no lifecycle of its own, so opening and closing windows is the single source of truth.
 */
class DeviceSessionRegistry {
    private val _sessions = MutableStateFlow<List<DeviceSessionHandle>>(emptyList())
    val sessions: StateFlow<List<DeviceSessionHandle>> = _sessions.asStateFlow()

    /** Replaces the whole set. Called whenever the window list changes. */
    fun update(handles: List<DeviceSessionHandle>) {
        _sessions.value = handles
    }

    /**
     * Resolves a tool's optional `deviceId` to a session.
     *
     * With one window open, [deviceId] may be omitted and defaults to it. With several open, an
     * absent or unknown [deviceId] returns null so the tool can tell the agent to pass one and list
     * the choices — the same rule the plan settled on.
     */
    fun resolve(deviceId: String?): DeviceSessionHandle? {
        val current = _sessions.value
        return when {
            deviceId != null -> current.firstOrNull { it.deviceId == deviceId }
            current.size == 1 -> current.first()
            else -> null
        }
    }
}
