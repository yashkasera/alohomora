package io.github.yashkasera.alohomora.domain.usecase.api

import io.github.yashkasera.alohomora.domain.repository.LogRepository

internal class GetLogsUseCase(private val logRepository: LogRepository) {
//    operator fun invoke(): Flow<List<LogEntity>> {
//        return logRepository.getAllLogs()
//    }
}
