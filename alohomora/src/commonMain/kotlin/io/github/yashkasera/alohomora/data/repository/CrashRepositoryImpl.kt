package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.entity.Crash
import io.github.yashkasera.alohomora.domain.repository.CrashRepository
import kotlinx.coroutines.flow.Flow

internal class CrashRepositoryImpl(private val db: AlohomoraDb) : CrashRepository {
    override fun getAllCrashes(query: String, page: Int, pageSize: Int): Flow<List<Crash>> =
        db.crashDao().getAll(query, page, pageSize)

    override fun getCrashById(id: Long): Flow<Crash?> =
        db.crashDao().getById(id)

    override suspend fun insertCrash(crash: Crash): Long =
        db.crashDao().insert(crash)

    override suspend fun deleteCrash(crash: Crash) =
        db.crashDao().delete(crash)

    override suspend fun clearAll() =
        db.crashDao().clear()

    override suspend fun markAsViewed(id: Long) =
        db.crashDao().markAsViewed(id)
}
