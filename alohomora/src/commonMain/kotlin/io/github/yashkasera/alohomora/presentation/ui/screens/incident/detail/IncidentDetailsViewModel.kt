package io.github.yashkasera.alohomora.presentation.ui.screens.incident.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Incident
import io.github.yashkasera.alohomora.domain.usecase.incident.GetIncidentDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.incident.MarkIncidentAsViewedUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class IncidentDetailsState(
    val incident: Incident? = null,
    val isLoading: Boolean = true,
)

internal class IncidentDetailsViewModel(
    private val incidentId: Long,
    getIncidentDetailsUseCase: GetIncidentDetailsUseCase,
    private val markIncidentAsViewedUseCase: MarkIncidentAsViewedUseCase,
) : ViewModel() {

    init {
        markAsViewed()
    }

    val state: StateFlow<IncidentDetailsState> = getIncidentDetailsUseCase(incidentId)
        .map { incident ->
            IncidentDetailsState(
                incident = incident,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = IncidentDetailsState(),
        )

    private fun markAsViewed() {
        viewModelScope.launch {
            markIncidentAsViewedUseCase(incidentId)
        }
    }
}
