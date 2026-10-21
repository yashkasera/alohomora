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

internal data class TrafficDetailsState(
    val trafficEntry: TrafficEntry? = null,
    val isLoading: Boolean = true,
    val showShareSheet: Boolean = false,
    val showSlackSheet: Boolean = false,
    val isSlackConfigured: Boolean = AlohomoraImpl.config?.slackWebhookUrl.isNullOrBlank().not(),
    val shareError: String? = null,
    val replayResultTraceId: String? = null,
) {
    val replayBlockedReason: ReplayBlockedReason?
        get() = trafficEntry?.replayBlockedReason()

    val canReplay: Boolean
        get() = Alohomora.isReplaySupported && trafficEntry != null && replayBlockedReason == null
}

internal class TrafficDetailsViewModel(
    private val trafficId: String,
    private val shareManager: ShareManager,
    private val slackShareService: SlackShareService,
    getTrafficDetailsUseCase: GetTrafficDetailsUseCase,
    private val markTrafficAsViewedUseCase: MarkTrafficAsViewedUseCase,
    observeReplayResultUseCase: ObserveReplayResultUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TrafficDetailsState())
    val state: StateFlow<TrafficDetailsState> = _state.asStateFlow()

    init {
        // Opening the detail screen is what "viewed" means here; the list dims the row on the
        // strength of it.
        viewModelScope.launch { markTrafficAsViewedUseCase(trafficId) }

        getTrafficDetailsUseCase(id = trafficId)
            .onEach { trafficEntry ->
                _state.value = _state.value.copy(trafficEntry = trafficEntry, isLoading = false)
            }
            .launchIn(viewModelScope)

        // Collected from the start rather than only after a replay: a replay of this traffic from
        // the *desktop* also lands here, and the mobile console should show the result either way.
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
        val trafficEntry = _state.value.trafficEntry ?: return
        shareManager.shareText(trafficEntry.curlCommand())
        hideShareSheet()
    }

    fun shareTextViaSystem() {
        val trafficEntry = _state.value.trafficEntry ?: return
        shareManager.shareText(trafficEntry.generateTransactionText())
        hideShareSheet()
    }

    fun shareFileViaSystem() {
        val trafficEntry = _state.value.trafficEntry ?: return
        shareManager.shareFile(
            content = trafficEntry.generateTransactionText(),
            filename = "request_${trafficEntry.id}.txt",
            mimeType = "text/plain",
        )
        hideShareSheet()
    }

    fun shareCurlToSlack(email: String) {
        val trafficEntry = _state.value.trafficEntry ?: return
        viewModelScope.launch {
            slackShareService.shareCurl(trafficEntry, email)
                .onSuccess {
                    hideSlackSheet()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(shareError = error.message)
                }
        }
    }

    fun shareTextToSlack(email: String) {
        val trafficEntry = _state.value.trafficEntry ?: return
        viewModelScope.launch {
            slackShareService.shareText(trafficEntry, email)
                .onSuccess {
                    hideSlackSheet()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(shareError = error.message)
                }
        }
    }

}
