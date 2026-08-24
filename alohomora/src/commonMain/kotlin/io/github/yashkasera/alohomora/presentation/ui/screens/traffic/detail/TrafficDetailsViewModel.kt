package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.domain.service.SlackShareService
import io.github.yashkasera.alohomora.domain.usecase.traffic.GetTrafficDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.MarkTrafficAsViewedUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.ObserveReplayResultUseCase
import io.github.yashkasera.alohomora.replay.ReplayBlockedReason
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.github.yashkasera.alohomora.utils.share.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal data class TraceDetailsState(
    val trace: TrafficEntry? = null,
    val isLoading: Boolean = true,
    val showShareSheet: Boolean = false,
    val showSlackSheet: Boolean = false,
    val isSlackConfigured: Boolean = AlohomoraImpl.config?.slackWebhookUrl.isNullOrBlank().not(),
    val shareError: String? = null,
    val replayResultTraceId: String? = null,
) {
    val replayBlockedReason: ReplayBlockedReason?
        get() = trace?.replayBlockedReason()

    val canReplay: Boolean
        get() = Alohomora.isReplaySupported && trace != null && replayBlockedReason == null
}

internal class TrafficDetailsViewModel(
    private val trafficId: String,
    private val shareManager: ShareManager,
    private val slackShareService: SlackShareService,
    getTraceDetailsUseCase: GetTrafficDetailsUseCase,
    private val markTraceAsViewedUseCase: MarkTrafficAsViewedUseCase,
    observeReplayResultUseCase: ObserveReplayResultUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TraceDetailsState())
    val state: StateFlow<TraceDetailsState> = _state.asStateFlow()

    init {
        // Opening the detail screen is what "viewed" means here; the list dims the row on the
        // strength of it.
        viewModelScope.launch { markTraceAsViewedUseCase(trafficId) }

        getTraceDetailsUseCase(id = trafficId)
            .onEach { trace ->
                _state.value = _state.value.copy(trace = trace, isLoading = false)
            }
            .launchIn(viewModelScope)

        // Collected from the start rather than only after a replay: a replay of this trace from the
        // *desktop* also lands here, and the mobile console should show the result either way.
        observeReplayResultUseCase(trafficId)
            .onEach { replay ->
                _state.value = _state.value.copy(replayResultTraceId = replay?.id)
            }
            .launchIn(viewModelScope)
    }

    fun showShareSheet() {
        _state.value = _state.value.copy(showShareSheet = true, shareError = null)
    }

    fun hideShareSheet() {
        _state.value = _state.value.copy(showShareSheet = false)
    }

    fun showSlackSheet() {
        _state.value = _state.value.copy(showSlackSheet = true, shareError = null)
    }

    fun hideSlackSheet() {
        _state.value = _state.value.copy(showSlackSheet = false)
    }

    fun shareCurlViaSystem() {
        val trace = _state.value.trace ?: return
        shareManager.shareText(trace.curlCommand())
        hideShareSheet()
    }

    fun shareTextViaSystem() {
        val trace = _state.value.trace ?: return
        shareManager.shareText(trace.generateTransactionText())
        hideShareSheet()
    }

    fun shareFileViaSystem() {
        val trace = _state.value.trace ?: return
        shareManager.shareFile(
            content = trace.generateTransactionText(),
            filename = "request_${trace.id}.txt",
            mimeType = "text/plain",
        )
        hideShareSheet()
    }

    fun shareCurlToSlack(email: String) {
        val trace = _state.value.trace ?: return
        viewModelScope.launch {
            slackShareService.shareCurl(trace, email)
                .onSuccess {
                    hideSlackSheet()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(shareError = error.message)
                }
        }
    }

    fun shareTextToSlack(email: String) {
        val trace = _state.value.trace ?: return
        viewModelScope.launch {
            slackShareService.shareText(trace, email)
                .onSuccess {
                    hideSlackSheet()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(shareError = error.message)
                }
        }
    }

}
