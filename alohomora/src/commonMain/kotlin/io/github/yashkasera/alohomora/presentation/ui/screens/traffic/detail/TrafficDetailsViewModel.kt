package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.domain.service.SlackShareService
import io.github.yashkasera.alohomora.domain.usecase.traffic.GetTrafficDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.MarkTrafficAsViewedUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.ObserveReplayResultUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.ReplayTrafficUseCase
import io.github.yashkasera.alohomora.replay.ReplayBlockedReason
import io.github.yashkasera.alohomora.replay.ReplayOutcome
import io.github.yashkasera.alohomora.replay.ReplayRequest
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.github.yashkasera.alohomora.replay.toReplayRequest
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
    val isSlackConfigured: Boolean = Alohomora.config?.slackWebhookUrl.isNullOrBlank().not(),
    val shareError: String? = null,
    val showReplaySheet: Boolean = false,
    val isReplaying: Boolean = false,
    val replayError: String? = null,
    /**
     * The trace produced by the last replay, once it lands in the database.
     *
     * Drives the "view result" affordance. Held rather than navigated to automatically: yanking the
     * user to a different screen the instant a response arrives loses the payload they just edited
     * and may want to tweak again.
     */
    val replayResultTraceId: String? = null,
) {
    /** Why replay is unavailable for this trace, or null when it can be replayed. */
    val replayBlockedReason: ReplayBlockedReason?
        get() = trace?.replayBlockedReason()

    val canReplay: Boolean
        get() = Alohomora.isReplaySupported && trace != null && replayBlockedReason == null
}

internal class TrafficDetailsViewModel(
    private val traceId: String,
    private val shareManager: ShareManager,
    private val slackShareService: SlackShareService,
    getTraceDetailsUseCase: GetTrafficDetailsUseCase,
    private val markTraceAsViewedUseCase: MarkTrafficAsViewedUseCase,
    private val replayTraceUseCase: ReplayTrafficUseCase,
    observeReplayResultUseCase: ObserveReplayResultUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TraceDetailsState())
    val state: StateFlow<TraceDetailsState> = _state.asStateFlow()

    init {
        // Opening the detail screen is what "viewed" means here; the list dims the row on the
        // strength of it.
        viewModelScope.launch { markTraceAsViewedUseCase(traceId) }

        getTraceDetailsUseCase(id = traceId)
            .onEach { trace ->
                _state.value = _state.value.copy(trace = trace, isLoading = false)
            }
            .launchIn(viewModelScope)

        // Collected from the start rather than only after a replay: a replay of this trace from the
        // *desktop* also lands here, and the mobile console should show the result either way.
        observeReplayResultUseCase(traceId)
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

    fun clearError() {
        _state.value = _state.value.copy(shareError = null)
    }

    // ── Replay ───────────────────────────────────────────────────────────────

    /**
     * Opens the replay editor, seeded from the captured request.
     *
     * Returns the request to edit rather than storing it in state: the sheet owns the user's edits
     * from that point on, and mirroring them into the ViewModel would mean two copies of the same
     * draft that have to be kept in step for no benefit.
     */
    fun startReplay(): ReplayRequest? {
        val request = _state.value.trace?.toReplayRequest() ?: return null
        _state.value = _state.value.copy(showReplaySheet = true, replayError = null)
        return request
    }

    fun hideReplaySheet() {
        _state.value = _state.value.copy(showReplaySheet = false, replayError = null)
    }

    /**
     * Sends [request] through the host app's client.
     *
     * The sheet closes only on success. A failure here is usually something in the form — a URL
     * edited into an invalid one, a host that will not resolve — so discarding the edits along with
     * the error would make the mistake harder to fix, not easier.
     */
    fun replay(request: ReplayRequest) {
        if (_state.value.isReplaying) return
        _state.value = _state.value.copy(isReplaying = true, replayError = null)
        viewModelScope.launch {
            when (val outcome = replayTraceUseCase(request)) {
                is ReplayOutcome.Sent -> _state.value = _state.value.copy(
                    isReplaying = false,
                    showReplaySheet = false,
                )

                is ReplayOutcome.Failed -> _state.value = _state.value.copy(
                    isReplaying = false,
                    replayError = outcome.reason,
                )
            }
        }
    }

    fun clearReplayError() {
        _state.value = _state.value.copy(replayError = null)
    }
}
