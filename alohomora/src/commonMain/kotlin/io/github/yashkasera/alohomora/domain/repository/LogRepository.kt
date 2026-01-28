package io.github.yashkasera.alohomora.domain.repository

internal interface LogRepository {
//    fun getAllLogs(): Flow<List<LogEntity>>
//
//    suspend fun addLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)

    suspend fun clear()
}
