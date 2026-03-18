package io.github.yashkasera.alohomora.presentation.ui.screens.trace.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.domain.service.SlackShareService
import io.github.yashkasera.alohomora.domain.usecase.trace.GetTraceDetailsUseCase
import io.github.yashkasera.alohomora.utils.share.ShareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class TraceDetailsState(
    val trace: TraceEntry? = null,
    val isLoading: Boolean = true,
    val showShareSheet: Boolean = false,
    val showSlackSheet: Boolean = false,
    val isSlackConfigured: Boolean = Alohomora.config?.slackWebhookUrl.isNullOrBlank().not(),
    val shareError: String? = null,
)

internal class TraceDetailsViewModel(
    private val traceId: String,
    private val shareManager: ShareManager,
    private val slackShareService: SlackShareService,
    getTraceDetailsUseCase: GetTraceDetailsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TraceDetailsState())
    val state: StateFlow<TraceDetailsState> = _state.asStateFlow()

    init {
        getTraceDetailsUseCase(id = traceId)
            .onEach { trace ->
                _state.value = _state.value.copy(trace = trace, isLoading = false)
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
}
