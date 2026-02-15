package io.github.yashkasera.alohomora.domain.usecase.events

import io.github.yashkasera.alohomora.common.Analytics
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

internal class GetEventsUseCase(private val eventRepository: EventRepository) {
    operator fun invoke(): Flow<List<Analytics>> {
        return eventRepository.getAllEvents()
    }
}
