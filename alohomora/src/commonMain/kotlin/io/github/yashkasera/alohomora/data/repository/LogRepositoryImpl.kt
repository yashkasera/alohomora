package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

internal class LogRepositoryImpl(
    private val db: AlohomoraDb,
) : LogRepository {
    //    override fun getAllLogs(): Flow<List<LogEntity>> = db.logDao().getAllLogs()
//
//    override suspend fun addLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
//        db.logDao().insert(
//            LogEntity(
//                timestamp = Clock.System.now().toEpochMilliseconds(),
//                level = level,
//                tag = tag,
//                message = message,
//                throwableStacktrace = throwable?.stackTraceToString()
//            )
//        )
//    }
//
//    override suspend fun clear() = db.logDao().clear()
    override suspend fun clear() {

    }
}
