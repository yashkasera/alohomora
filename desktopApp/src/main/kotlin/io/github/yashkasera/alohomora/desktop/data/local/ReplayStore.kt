package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.ReplayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReplayStore {
    private val _state = MutableStateFlow(ReplayState())
    val state: StateFlow<ReplayState> = _state.asStateFlow()

    fun setSupported(supported: Boolean) {
        _state.value = _state.value.copy(supported = supported)
    }

    /** Marks a replay as sent, and drops any previous failure so a retry starts clean. */
    fun markInFlight(sourceTraceId: String) {
        _state.value = _state.value.copy(
            inFlight = _state.value.inFlight + sourceTraceId,
            errors = _state.value.errors - sourceTraceId,
        )
    }

    fun markSucceeded(sourceTraceId: String) {
        _state.value = _state.value.copy(inFlight = _state.value.inFlight - sourceTraceId)
    }

    fun markFailed(sourceTraceId: String, error: String) {
        _state.value = _state.value.copy(
            inFlight = _state.value.inFlight - sourceTraceId,
            errors = _state.value.errors + (sourceTraceId to error),
        )
    }

    fun dismissError(sourceTraceId: String) {
        _state.value = _state.value.copy(errors = _state.value.errors - sourceTraceId)
    }

    /**
     * Fails every in-flight replay because the connection carrying them is gone.
     *
     * Without this a replay that was out on the wire when the device dropped keeps its spinner
     * forever: the `ReplayResultMessage` that would have cleared it died with the socket, and the
     * action stays disabled even after the session reconnects.
     */
    fun abandonInFlight() {
        val inFlight = _state.value.inFlight
        if (inFlight.isEmpty()) return
        _state.value = _state.value.copy(
            inFlight = emptySet(),
            errors = _state.value.errors + inFlight.associateWith { CONNECTION_LOST },
        )
    }

    private companion object {
        const val CONNECTION_LOST = "The device disconnected before the replay reported back."
    }

    /**
     * Resets everything except [ReplayState.supported], which the next snapshot re-establishes.
     *
     * In-flight entries have to go: the connection they were waiting on is gone, so their
     * [io.github.yashkasera.alohomora.common.ReplayResultMessage] is never arriving and the action
     * would stay stuck in a spinner.
     */
    fun clear() {
        _state.value = ReplayState()
    }
}
