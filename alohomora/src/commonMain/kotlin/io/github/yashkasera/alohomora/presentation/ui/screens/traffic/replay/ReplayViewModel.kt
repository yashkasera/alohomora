package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.replay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.domain.usecase.traffic.GetTrafficDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.ObserveReplayResultUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.ReplayTrafficUseCase
import io.github.yashkasera.alohomora.replay.ReplayBlockedReason
import io.github.yashkasera.alohomora.replay.ReplayHeaderText
import io.github.yashkasera.alohomora.replay.ReplayOutcome
import io.github.yashkasera.alohomora.replay.ReplayRequest
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.github.yashkasera.alohomora.replay.toReplayRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal data class ReplayState(
    val isLoading: Boolean = true,
    val blockedReason: ReplayBlockedReason? = null,
    val sourceTrace: TrafficEntry? = null,
    val method: String = "",
    val url: String = "",
    val headers: String = "",
    val body: String = "",
    val contentType: String? = null,
    val isReplaying: Boolean = false,
    val replayError: String? = null,
    /** The request was sent; the screen shows the "opening result" state while the result is located. */
    val replaySent: Boolean = false,
    /** The replayed entry that arrived after [replaySent]; navigate straight to it when non-null. */
    val resultTraceId: String? = null,
    /** No replayed entry was located in time (desktop-triggered or a handler that cannot stamp the
     *  `replayOf` link). The screen falls back to the source entry, where the banner still catches it. */
    val replayTimedOut: Boolean = false,
)

internal class ReplayViewModel(
    private val trafficId: String,
    getTrafficDetailsUseCase: GetTrafficDetailsUseCase,
    private val replayTrafficUseCase: ReplayTrafficUseCase,
    private val observeReplayResultUseCase: ObserveReplayResultUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReplayState())
    val state: StateFlow<ReplayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val trace = getTrafficDetailsUseCase(trafficId).first()
            if (trace == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            val blocked = trace.replayBlockedReason()
            if (blocked != null || !Alohomora.isReplaySupported) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    sourceTrace = trace,
                    blockedReason = blocked ?: ReplayBlockedReason.INCOMPLETE_TRACE,
                )
                return@launch
            }

            val request = trace.toReplayRequest()!!
            _state.value = _state.value.copy(
                isLoading = false,
                sourceTrace = trace,
                method = request.method,
                url = request.url,
                headers = ReplayHeaderText.render(request.headers),
                body = request.body.orEmpty(),
                contentType = request.contentType,
            )
        }
    }

    fun updateMethod(value: String) {
        _state.value = _state.value.copy(method = value.uppercase())
    }

    fun updateUrl(value: String) {
        _state.value = _state.value.copy(url = value)
    }

    fun updateHeaders(value: String) {
        _state.value = _state.value.copy(headers = value)
    }

    fun updateBody(value: String) {
        _state.value = _state.value.copy(body = value)
    }

    fun send() {
        val s = _state.value
        if (s.isReplaying || s.method.isBlank() || s.url.isBlank()) return
        _state.value = s.copy(isReplaying = true, replayError = null)

        viewModelScope.launch {
            // The id of the newest replay of this source that already exists, so a replay done
            // earlier (or a stale row) is never mistaken for the one this Send produces.
            val baselineReplayId = observeReplayResultUseCase(trafficId).first()?.id

            val request = ReplayRequest(
                sourceTraceId = trafficId,
                method = s.method.trim().uppercase(),
                url = s.url.trim(),
                headers = ReplayHeaderText.parse(s.headers),
                body = s.body.takeIf { it.isNotBlank() },
                contentType = s.contentType,
            )
            when (val outcome = replayTrafficUseCase(request)) {
                is ReplayOutcome.Sent -> {
                    _state.value = _state.value.copy(isReplaying = false, replaySent = true)
                    awaitReplayResult(baselineReplayId)
                }

                is ReplayOutcome.Failed -> _state.value = _state.value.copy(
                    isReplaying = false,
                    replayError = outcome.reason,
                )
            }
        }
    }

    /**
     * Waits for the interceptor to capture the replayed request and link it to this source via
     * `replayOf`, then exposes its id so the screen can open it directly.
     *
     * The handlers carry no trace id in [ReplayOutcome.Sent] — the replayed entry arrives
     * asynchronously on its own coroutine — so the only way to find it is to watch the
     * `replayOf` relation for a row newer than the one that existed before Send. When no row
     * arrives within [RESULT_TIMEOUT_MILLIS] (a desktop-triggered replay, or a handler that cannot
     * stamp the link) the screen falls back to the source entry, where the banner still catches it.
     */
    private suspend fun awaitReplayResult(baselineReplayId: String?) {
        val resultId = withTimeoutOrNull(RESULT_TIMEOUT_MILLIS) {
            observeReplayResultUseCase(trafficId)
                .mapNotNull { replay -> replay?.id?.takeIf { it != baselineReplayId } }
                .first()
        }
        _state.value = _state.value.copy(
            resultTraceId = resultId,
            replayTimedOut = resultId == null,
        )
    }

    private companion object {
        /**
         * Short on purpose: long enough for the interceptor to persist the replayed row on a
         * healthy device, short enough that a replay which never stamps its link (desktop-triggered,
         * or the known Android `replayOf` gap) falls back to the source entry without a long hang.
         */
        const val RESULT_TIMEOUT_MILLIS = 2_000L
    }
}
