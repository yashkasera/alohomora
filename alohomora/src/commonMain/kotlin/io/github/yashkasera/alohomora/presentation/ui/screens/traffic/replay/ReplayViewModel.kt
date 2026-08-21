package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.replay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.domain.usecase.traffic.GetTrafficDetailsUseCase
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
import kotlinx.coroutines.launch

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
    val replaySent: Boolean = false,
)

internal class ReplayViewModel(
    private val trafficId: String,
    getTrafficDetailsUseCase: GetTrafficDetailsUseCase,
    private val replayTrafficUseCase: ReplayTrafficUseCase,
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
            val request = ReplayRequest(
                sourceTraceId = trafficId,
                method = s.method.trim().uppercase(),
                url = s.url.trim(),
                headers = ReplayHeaderText.parse(s.headers),
                body = s.body.takeIf { it.isNotBlank() },
                contentType = s.contentType,
            )
            when (val outcome = replayTrafficUseCase(request)) {
                is ReplayOutcome.Sent -> _state.value = _state.value.copy(
                    isReplaying = false,
                    replaySent = true,
                )

                is ReplayOutcome.Failed -> _state.value = _state.value.copy(
                    isReplaying = false,
                    replayError = outcome.reason,
                )
            }
        }
    }
}
