package io.github.yashkasera.alohomora.domain.usecase

import io.github.yashkasera.alohomora.data.entity.Analytics
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

internal class GetEventsUseCase(private val eventRepository: EventRepository) {
    operator fun invoke(): Flow<List<Analytics>> {
        return eventRepository.getAllEvents()
    }
}
