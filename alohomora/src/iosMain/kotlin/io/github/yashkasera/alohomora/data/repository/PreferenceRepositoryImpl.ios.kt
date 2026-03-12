package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.domain.model.PreferenceEntry
import io.github.yashkasera.alohomora.domain.model.PreferenceSource
import io.github.yashkasera.alohomora.domain.model.PreferenceStore
import io.github.yashkasera.alohomora.domain.model.PreferenceType
import io.github.yashkasera.alohomora.domain.repository.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PreferenceRepository.
 *
 * Reads preferences from NSUserDefaults.
 */
internal class PreferenceRepositoryImpl : PreferenceRepository {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val _preferencesFlow = MutableStateFlow<List<PreferenceEntry>>(emptyList())
    private var cachedPreferences: List<PreferenceEntry> = emptyList()

    override fun observeAllPreferences(): Flow<List<PreferenceEntry>> = _preferencesFlow.asStateFlow()

    override suspend fun getAllPreferences(): List<PreferenceEntry> {
        if (cachedPreferences.isEmpty()) {
            cachedPreferences = scanAllPreferences()
            _preferencesFlow.value = cachedPreferences
        }
        return cachedPreferences
    }

    override suspend fun refresh(): List<PreferenceEntry> {
        cachedPreferences = scanAllPreferences()
        _preferencesFlow.value = cachedPreferences
        return cachedPreferences
    }

    override suspend fun getStores(): List<PreferenceStore> = withContext(Dispatchers.Default) {
        listOf(
            PreferenceStore(
                name = "NSUserDefaults",
                source = PreferenceSource.NS_USER_DEFAULTS,
                isEncrypted = false,
                entryCount = getEntryCount(),
            )
        )
    }

    override fun getTotalSize(entries: List<PreferenceEntry>): String {
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

    private suspend fun scanAllPreferences(): List<PreferenceEntry> = withContext(Dispatchers.Default) {
        val entries = mutableListOf<PreferenceEntry>()
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
                PreferenceEntry(
                    key = keyString,
                    value = stringValue,
                    type = PreferenceType.detect(stringValue),
                    source = PreferenceSource.NS_USER_DEFAULTS,
                    isEncrypted = false,
                    storeName = "NSUserDefaults",
                )
            )
        }

        entries.sortedBy { it.key }
    }
}
