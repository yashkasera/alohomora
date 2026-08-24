package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.model.CacheSource
import io.github.yashkasera.alohomora.domain.model.CacheStore
import io.github.yashkasera.alohomora.domain.model.CacheType
import io.github.yashkasera.alohomora.domain.repository.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of CacheRepository.
 *
 * Reads preferences from NSUserDefaults.
 */
internal class CacheRepositoryImpl : CacheRepository {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val _preferencesFlow = MutableStateFlow<List<CacheEntry>>(emptyList())
    private var cachedPreferences: List<CacheEntry> = emptyList()

    override fun observeAllPreferences(): Flow<List<CacheEntry>> = _preferencesFlow.asStateFlow()

    override suspend fun getAllPreferences(): List<CacheEntry> {
        if (cachedPreferences.isEmpty()) {
            cachedPreferences = scanAllPreferences()
            _preferencesFlow.value = cachedPreferences
        }
        return cachedPreferences
    }

    override suspend fun refresh(): List<CacheEntry> {
        cachedPreferences = scanAllPreferences()
        _preferencesFlow.value = cachedPreferences
        return cachedPreferences
    }

    override suspend fun getStores(): List<CacheStore> = withContext(Dispatchers.Default) {
        listOf(
            CacheStore(
                name = "NSUserDefaults",
                source = CacheSource.NS_USER_DEFAULTS,
                isEncrypted = false,
                entryCount = getEntryCount(),
            ),
        )
    }

    override fun getTotalSize(entries: List<CacheEntry>): String {
        val totalBytes = entries.sumOf { entry ->
            entry.key.encodeToByteArray().size + entry.value.encodeToByteArray().size
        }

        return when {
            totalBytes < 1024 -> "$totalBytes B"
            totalBytes < 1024 * 1024 -> "${totalBytes / 1024} KB"
            else -> {
                val mb = totalBytes / (1024.0 * 1024.0)
                val rounded = kotlin.math.round(mb * 10) / 10
                "$rounded MB"
            }
        }
    }

    private fun getEntryCount(): Int {
        return defaults.dictionaryRepresentation().size
    }

    override suspend fun updateEntry(
        storeName: String,
        key: String,
        value: String?,
        type: String,
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            when (type) {
                "STRING" -> defaults.setObject(value, forKey = key)
                "INT" -> defaults.setInteger(value!!.toLong(), forKey = key)
                "BOOLEAN" -> defaults.setBool(value!!.toBoolean(), forKey = key)
                "LONG" -> defaults.setInteger(value!!.toLong(), forKey = key)
                "FLOAT" -> defaults.setFloat(value!!.toFloat(), forKey = key)
                else -> return@withContext false
            }
            defaults.synchronize()
            invalidateCache()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteEntry(storeName: String, key: String): Boolean =
        withContext(Dispatchers.Default) {
            try {
                defaults.removeObjectForKey(key)
                defaults.synchronize()
                invalidateCache()
                true
            } catch (_: Exception) {
                false
            }
        }

    private fun invalidateCache() {
        cachedPreferences = emptyList()
    }

    private suspend fun scanAllPreferences(): List<CacheEntry> = withContext(Dispatchers.Default) {
        val entries = mutableListOf<CacheEntry>()
        val dict = defaults.dictionaryRepresentation()

        dict.forEach { (key, value) ->
            val keyString = key.toString()

            val stringValue = when (value) {
                null -> "null"
                is Set<*> -> value.joinToString(prefix = "[", postfix = "]")
                is List<*> -> value.joinToString(prefix = "[", postfix = "]")
                else -> value.toString()
            }

            entries.add(
                CacheEntry(
                    key = keyString,
                    value = stringValue,
                    type = CacheType.detect(stringValue),
                    source = CacheSource.NS_USER_DEFAULTS,
                    isEncrypted = false,
                    storeName = "NSUserDefaults",
                ),
            )
        }

        entries.sortedBy { it.key }
    }
}
